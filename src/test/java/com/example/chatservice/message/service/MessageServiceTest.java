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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

    @MockitoBean
    MessageDeliveryService messageDeliveryService;

    @MockitoBean
    ChatMessageProducer chatMessageProducer;

    @MockitoBean
    PendingLastMessageFlushService pendingLastMessageFlushService;

    @MockitoBean
    HotRoomDetectionService hotRoomDetectionService;

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

    @BeforeEach
    void setUp() {
        // 기본적으로 hot room이 아니라고 설정
        when(hotRoomDetectionService.isHotRoom(anyLong())).thenReturn(false);
        when(hotRoomDetectionService.shouldSkipHotUpdate(anyLong())).thenReturn(false);
        when(hotRoomDetectionService.getDebounceDuration()).thenReturn(java.time.Duration.ofSeconds(3));

        // 클린업: 메시지 샤드/메인 엔티티 모두 정리
        messageRepository.deleteAll();
        readStatusRepository.deleteAll();
        userChatRepository.deleteAll();
        chatRepository.deleteAll();
        userRepository.deleteAll();
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
        assertThat(saved.getSenderId()).isEqualTo(sender.getId());
        assertThat(saved.getChatRoomId()).isEqualTo(chatRoomId);

        ChatRoom chatRoom = chatRepository.findById(chatRoomId).orElseThrow();
        UserChat uc = userChatRepository.findByUserAndChatRoomAndLeavedAtIsNull(sender, chatRoom).orElseThrow();
        assertThat(uc.getLastMessageId()).isEqualTo(saved.getId());

        ReadStatus rs = readStatusRepository.findByUserAndChatRoom(sender, chatRoom);
        assertThat(rs.getLastReadMessageId()).isEqualTo(saved.getId());

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

        when(hotRoomDetectionService.isHotRoom(chatRoomId)).thenReturn(true);
        when(hotRoomDetectionService.shouldSkipHotUpdate(chatRoomId)).thenReturn(true);
        when(hotRoomDetectionService.getDebounceDuration()).thenReturn(java.time.Duration.ofSeconds(3));

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

        Message m1 = messageRepository.save(Message.builder().id(1L).senderId(u1.getId()).chatRoomId(chatRoomId).message("m1").build());
        Message m2 = messageRepository.save(Message.builder().id(2L).senderId(u2.getId()).chatRoomId(chatRoomId).message("m2").build());
        Message m3 = messageRepository.save(Message.builder().id(3L).senderId(u1.getId()).chatRoomId(chatRoomId).message("m3").build());

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

        Message m1 = messageRepository.save(Message.builder().id(10L).senderId(u1.getId()).chatRoomId(chatRoomId).message("m1").build());
        Message m2 = messageRepository.save(Message.builder().id(20L).senderId(u2.getId()).chatRoomId(chatRoomId).message("m2").build());

        // when
        messageService.markMessagesAsRead(u1.getId(), chatRoomId, m2.getId());
        ReadStatus rs = readStatusRepository.findByUserAndChatRoom(u1, chatRoom);
        assertThat(rs).isNotNull();
        assertThat(rs.getLastReadMessageId()).isEqualTo(m2.getId());

        messageService.markMessagesAsRead(u1.getId(), chatRoomId, m1.getId());
        ReadStatus rsAfter = readStatusRepository.findByUserAndChatRoom(u1, chatRoom);
        assertThat(rsAfter.getLastReadMessageId()).isEqualTo(m2.getId());
    }
}