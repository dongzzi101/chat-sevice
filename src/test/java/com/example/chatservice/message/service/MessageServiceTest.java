package com.example.chatservice.message.service;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ReadStatus;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ReadStatusRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.chat.sercivce.ChatService;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.event.ChatMessageEvent;
import com.example.chatservice.message.kafka.ChatMessageProducer;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.data.redis.repositories.enabled=false")
@ActiveProfiles("test")
@Transactional
class MessageServiceTest {

    /**
     * 1. 메시지 보내기
     * 1-1. 정상 전송
     * 1-2. 동기/비동기 호출
     * 1-3. hot room 분기 되는지
     * ------------------------
     * 2. 메시지 읽기
     * 2-1. 메시지 잘 가져오는지
     * ------------------------
     * 3. 읽음 처리
     */

    @MockBean
    RedisTemplate<String, Object> redisTemplate;

    @MockBean
    MessageDeliveryService messageDeliveryService;

    @MockBean
    ChatMessageProducer chatMessageProducer;

    @MockBean
    PendingLastMessageFlushService pendingLastMessageFlushService;

    @MockBean
    RedisConnectionFactory redisConnectionFactory;

    @Autowired
    MessageService messageService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ChatRepository chatRepository;
    @Autowired
    ChatService chatService;
    @Autowired
    MessageRepository messageRepository;
    @Autowired
    ReadStatusRepository readStatusRepository;
    @SpyBean
    UserChatRepository userChatRepository;

    ValueOperations<String, Object> valueOps;
    static final String MODE_KEY_PREFIX = "chat:%d:mode";
    static final String COUNT_KEY_PREFIX = "chat:%d:msgCount";
    static final String LAST_APPLIED_PREFIX = "chat:%d:lastApplied";

    @BeforeEach
    void setUp() {
        valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);
        when(valueOps.get(anyString())).thenReturn(null);
        when(redisTemplate.expire(anyString(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("메시지 전송을 확인한다.")
    void sendMessageTest() {
        // given
        User sender = userRepository.save(User.builder().username("userA").build());
        User other = userRepository.save(User.builder().username("userB").build());
        Long chatRoomId = chatService.createChatRoom(sender.getId(), new ChatRequest(List.of(other.getId()))).getId();

        // when
        messageService.sendMessageViaWebSocket(sender.getId(), other.getId(), chatRoomId, "hello");

        // 트랜잭션 커밋 훅 실행 (테스트는 @Transactional로 롤백되므로 수동 호출)
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        // then
        Message saved = messageRepository.findAll().get(0);
        assertThat(saved.getMessage()).isEqualTo("hello");
        assertThat(saved.getSender().getId()).isEqualTo(sender.getId());
        assertThat(saved.getChatRoom().getId()).isEqualTo(chatRoomId);

        UserChat uc = userChatRepository.findByUserAndChatRoomAndLeavedAtIsNull(sender, saved.getChatRoom()).orElseThrow();
        assertThat(uc.getLastMessageId()).isEqualTo(saved.getId());

        ReadStatus rs = readStatusRepository.findByUserAndChatRoom(sender, saved.getChatRoom());
        assertThat(rs.getLastReadMessage().getId()).isEqualTo(saved.getId());

        verify(messageDeliveryService).deliverMessage(eq(sender.getId()), any(Message.class));
        verify(chatMessageProducer).sendMessage(any(ChatMessageEvent.class));
    }

    @Test
    @DisplayName("hot room이면 lastMessageId 업데이트를 스킵하고 flush를 예약한다.")
    void sendMessageHotRoomTest() {
        // given
        User sender = userRepository.save(User.builder().username("userA").build());
        User other = userRepository.save(User.builder().username("userB").build());
        Long chatRoomId = chatService.createChatRoom(sender.getId(), new ChatRequest(List.of(other.getId()))).getId();

        when(valueOps.increment(COUNT_KEY_PREFIX.formatted(chatRoomId))).thenReturn(10L);
        when(valueOps.get(MODE_KEY_PREFIX.formatted(chatRoomId))).thenReturn("hot");
        when(valueOps.get(LAST_APPLIED_PREFIX.formatted(chatRoomId))).thenReturn(String.valueOf(System.currentTimeMillis()));

        // when
        messageService.sendMessageViaWebSocket(sender.getId(), other.getId(), chatRoomId, "hot");
        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        // then
        verify(pendingLastMessageFlushService).scheduleFlush(eq(chatRoomId), anyLong(), any());
        verify(userChatRepository, Mockito.never()).updateLastMessageIdForChat(anyLong(), anyLong());
    }

    @Test
    @DisplayName("getMessages는 before/after를 합쳐 시간 순으로 반환한다.")
    void getMessagesTest() {
        // given
        User u1 = userRepository.save(User.builder().username("u1").build());
        User u2 = userRepository.save(User.builder().username("u2").build());
        Long chatRoomId = chatService.createChatRoom(u1.getId(), new ChatRequest(List.of(u2.getId()))).getId();
        ChatRoom chatRoom = chatRepository.findById(chatRoomId).orElseThrow();

        Message m1 = messageRepository.save(Message.builder().id(1L).sender(u1).chatRoom(chatRoom).message("m1").build());
        Message m2 = messageRepository.save(Message.builder().id(2L).sender(u2).chatRoom(chatRoom).message("m2").build());
        Message m3 = messageRepository.save(Message.builder().id(3L).sender(u1).chatRoom(chatRoom).message("m3").build());

        // when
        List<MessageResponse> responses = messageService.getMessages(u1.getId(), chatRoomId, m2.getId(), 1, 1);

        // then
        assertThat(responses).hasSize(3);
        assertThat(responses.get(0).getMessage()).isEqualTo("m1");
        assertThat(responses.get(1).getMessage()).isEqualTo("m2");
        assertThat(responses.get(2).getMessage()).isEqualTo("m3");
    }

    @Test
    @DisplayName("markMessagesAsRead는 ReadStatus를 생성, 업데이트하고 뒤로 가지 않는다.")
    void markMessagesAsReadTest() {
        // given
        User u1 = userRepository.save(User.builder().username("u1").build());
        User u2 = userRepository.save(User.builder().username("u2").build());
        Long chatRoomId = chatService.createChatRoom(u1.getId(), new ChatRequest(List.of(u2.getId()))).getId();
        ChatRoom chatRoom = chatRepository.findById(chatRoomId).orElseThrow();

        Message m1 = messageRepository.save(Message.builder().id(10L).sender(u1).chatRoom(chatRoom).message("m1").build());
        Message m2 = messageRepository.save(Message.builder().id(20L).sender(u2).chatRoom(chatRoom).message("m2").build());

        // when
        messageService.markMessagesAsRead(u1.getId(), chatRoomId, m2.getId());
        ReadStatus rs = readStatusRepository.findByUserAndChatRoom(u1, chatRoom);
        assertThat(rs).isNotNull();
        assertThat(rs.getLastReadMessage().getId()).isEqualTo(m2.getId());

        messageService.markMessagesAsRead(u1.getId(), chatRoomId, m1.getId());
        ReadStatus rsAfter = readStatusRepository.findByUserAndChatRoom(u1, chatRoom);
        assertThat(rsAfter.getLastReadMessage().getId()).isEqualTo(m2.getId());
    }
}