package com.example.chatservice.chat.sercivce;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.controller.request.ChatRoomResponse;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatType;
import com.example.chatservice.chat.entity.ReadStatus;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ReadStatusRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.exception.UserAlreadyJoinedException;
import com.example.chatservice.exception.UserNotFoundException;
import com.example.chatservice.exception.UserNotJoinedException;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatServiceTest {

    /**
     * 1. 채팅방 생성
     * 1-1. IM, GROUP, DIRECT
     * 1-2. 채팅방 중복 생성 방지
     * 1-3. 존재하지 않은 userId 들어오면 error 발생
     * <p>
     * 2. 채팅방 목록
     * 2-1. 채팅방 목록 정상 조회
     * 2-2. lastMessageId 기준으로 Ordering
     * <p>
     * 3. 채팅방 가입
     * 3-1. 채팅방 정상 가입 & 이미 가입된 유저가 다시 가입을 할 때 UserAlreadyJoinedException 발생
     * 3-2. chatKey 업데이트
     * <p>
     * 4. 채팅방 나가기
     * 4-1. 채팅방 정상적으로 잘 나가기 & 채팅방에 참여중이지 않은 유저 채팅방을 나가면 UserNotJoinedException 발생
     * 4-2. chatKey 업데이트
     * <p>
     * 5. chat key
     * 5-1. 같은 유저 조합이면 동일한 chat key 가 생성
     */

    @Autowired
    ChatService chatService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    UserChatRepository userChatRepository;

    @Autowired
    ReadStatusRepository readStatusRepository;

    User user1;
    User user2;
    User user3;
    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(User.builder().username("user1").build());
        user2 = userRepository.save(User.builder().username("user2").build());
        user3 = userRepository.save(User.builder().username("user3").build());
    }

    @Test
    @DisplayName("일대일 채팅방을 생성하면 ChatType.DIRECT 채팅방이 만들어진다.")
    void createChatRoom() {
        // given
        ChatRequest request = new ChatRequest(List.of(user2.getId()));

        // when
        ChatResponse response =
                chatService.createChatRoom(user1.getId(), request);

        // then
        ChatRoom chatRoom =
                chatRepository.findById(response.getId()).orElseThrow();

        assertThat(chatRoom.getType()).isEqualTo(ChatType.DIRECT);
        assertThat(userChatRepository.count()).isEqualTo(2);
        assertThat(readStatusRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("셀프 채팅방을 생성하면 ChatType.IM 채팅방이 만들어진다.")
    void createIMChatRoom() {
        // given
        ChatRequest request = new ChatRequest(List.of(user1.getId()));

        // when
        ChatResponse response =
                chatService.createChatRoom(user1.getId(), request);

        // then
        ChatRoom chatRoom =
                chatRepository.findById(response.getId()).orElseThrow();

        assertThat(chatRoom.getType()).isEqualTo(ChatType.IM);
        assertThat(userChatRepository.count()).isEqualTo(1);
        assertThat(readStatusRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("그룹 채팅방을 생성하면 ChatType.GROUP 채팅방이 만들어진다.")
    void createGroupChatRoom() {
        // given
        ChatRequest request = new ChatRequest(List.of(user1.getId(), user2.getId()));

        // when
        ChatResponse response =
                chatService.createChatRoom(user3.getId(), request);

        // then
        ChatRoom chatRoom =
                chatRepository.findById(response.getId()).orElseThrow();

        assertThat(chatRoom.getType()).isEqualTo(ChatType.GROUP);
        assertThat(userChatRepository.count()).isEqualTo(3);
        assertThat(readStatusRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("채팅방이 있는 경우 기존 채팅방을 반환한다.")
    void createUserChatRoom() {
        // given
        ChatRequest chatRequest1 = new ChatRequest(List.of(user1.getId(), user2.getId()));
        ChatResponse chatResponse1 = chatService.createChatRoom(user1.getId(), chatRequest1);
        ChatRoom chatRoom1 = chatRepository.findById(chatResponse1.getId()).orElseThrow();

        // when
        ChatRequest chatRequest2 = new ChatRequest(List.of(user1.getId(), user2.getId()));
        ChatResponse chatResponse2 = chatService.createChatRoom(user2.getId(), chatRequest2);
        ChatRoom chatRoom2 = chatRepository.findById(chatResponse2.getId()).orElseThrow();

        // then
        assertThat(chatRoom1.getId()).isEqualTo(chatRoom2.getId());
    }

    @Test
    @DisplayName("존재하지 않은 userId 가 들어오면 UserNotFoundException이 발생한다.")
    void createUserChatRoomWithException() {
        // given
        ChatRequest chatRequest = new ChatRequest(
                List.of(user1.getId(), user2.getId())
        );

        // when & then
        assertThrows(UserNotFoundException.class, () -> {
            chatService.createChatRoom(4L, chatRequest);
        });
    }

    @Test
    @DisplayName("채팅방 목록을 lastMessageId 기준으로 가져온다.")
    void getChatRoomsTest() {
        // given
        ChatRoom chatRoom1 = chatRepository.findById(
                chatService.createChatRoom(user1.getId(), new ChatRequest(List.of(user2.getId()))).getId()
        ).orElseThrow();
        ChatRoom chatRoom2 = chatRepository.findById(
                chatService.createChatRoom(user1.getId(), new ChatRequest(List.of(user3.getId()))).getId()
        ).orElseThrow();

        Message oldMessage = messageRepository.save(
                Message.builder()
                        .id(1L)
                        .message("old message")
                        .sender(user1)
                        .chatRoom(chatRoom1)
                        .build()
        );
        Message newMessage = messageRepository.save(
                Message.builder()
                        .id(2L)
                        .message("new message")
                        .sender(user1)
                        .chatRoom(chatRoom2)
                        .build()
        );

        userChatRepository.updateLastMessageIdForChat(chatRoom1.getId(), oldMessage.getId());
        userChatRepository.updateLastMessageIdForChat(chatRoom2.getId(), newMessage.getId());

        // when
        List<ChatRoomResponse> chatRooms = chatService.getChatRooms(user1.getId());

        // then
        assertThat(chatRooms).hasSize(2);
        assertThat(chatRooms.get(0).getChatRoomId()).isEqualTo(chatRoom2.getId());
        assertThat(chatRooms.get(0).getLastMessageId()).isEqualTo(newMessage.getId());
        assertThat(chatRooms.get(1).getChatRoomId()).isEqualTo(chatRoom1.getId());
        assertThat(chatRooms.get(1).getLastMessageId()).isEqualTo(oldMessage.getId());
    }

    @Test
    @DisplayName("새로운 유저가 채팅방을 가입하면 chatKey도 업데이트 된다.")
    void joinChatRoom() {
        // given
        ChatRoom chatRoom1 = chatRepository.findById(
                chatService.createChatRoom(user1.getId(), new ChatRequest(List.of(user2.getId()))).getId()
        ).orElseThrow();
        String beforeChatKey = chatRoom1.getChatKey();

        // when
        chatService.joinChatRoom(chatRoom1.getId(), user3.getId());

        // then
        List<UserChat> active = userChatRepository.findActiveByChatRoomWithUser(chatRoom1.getId());
        assertThat(active).hasSize(3);
        assertThat(active).anyMatch(uc -> uc.getUser().getId().equals(user3.getId()) && uc.getLeavedAt() == null);

        ReadStatus readStatus = readStatusRepository.findByUserAndChatRoom(user3, chatRoom1);
        assertThat(readStatus).isNotNull();
        assertThat(readStatus.getLastReadMessage()).isNull();

        ChatRoom newChatRoom = chatRepository.findById(chatRoom1.getId()).orElseThrow();
        assertThat(newChatRoom.getChatKey()).isNotEqualTo(beforeChatKey);
    }

    @Test
    @DisplayName("이미 가입된 채팅방이면 예외를 발생한다.")
    void joinChatRoom2() {
        // given
        ChatRoom chatRoom1 = chatRepository.findById(
                chatService.createChatRoom(user1.getId(), new ChatRequest(List.of(user2.getId()))).getId()
        ).orElseThrow();

        // when & then
        assertThrows(UserAlreadyJoinedException.class, () -> {
            chatService.joinChatRoom(chatRoom1.getId(), user1.getId());
        });
    }

    @Test
    @DisplayName("기존 유저가 채팅방을 나가면 chatKey도 업데이트 된다.")
    void leaveChatRoom() {
        // given
        ChatRoom chatRoom1 = chatRepository.findById(
                chatService.createChatRoom(user1.getId(), new ChatRequest(List.of(user2.getId(), user3.getId()))).getId()
        ).orElseThrow();
        String beforeChatKey = chatRoom1.getChatKey();
        Long leavingUserChatId = userChatRepository.findActiveByChatRoomWithUser(chatRoom1.getId()).stream()
                .filter(uc -> uc.getUser().getId().equals(user1.getId()))
                .map(UserChat::getId)
                .findFirst()
                .orElseThrow();

        // when
        chatService.leaveChatRoom(chatRoom1.getId(), user1.getId());

        // then
        List<UserChat> active = userChatRepository.findActiveByChatRoomWithUser(chatRoom1.getId());
        assertThat(active).hasSize(2);
        assertThat(active).noneMatch(uc -> uc.getUser().getId().equals(user1.getId()));

        ChatRoom newChatRoom = chatRepository.findById(chatRoom1.getId()).orElseThrow();
        UserChat leftUserChat = userChatRepository.findById(leavingUserChatId).orElseThrow();
        assertThat(leftUserChat.getLeavedAt()).isNotNull();
        assertThat(newChatRoom.getChatKey()).isNotEqualTo(beforeChatKey);
    }


    @Test
    @DisplayName("채팅방에 참여중이지 않은 유저가 채팅방을 나가면 UserNotJoinedException 발생한다.")
    void leaveChatRoomException() {
        // given
        ChatRoom chatRoom = chatRepository.findById(
                chatService.createChatRoom(user1.getId(), new ChatRequest(List.of(user2.getId()))).getId()
        ).orElseThrow();

        // when & then
        assertThrows(UserNotJoinedException.class, () -> {
            chatService.leaveChatRoom(chatRoom.getId(), user3.getId());
        });
    }

    @Test
    @DisplayName("userIds이 동일하다면 항상 같은 chatKey를 반환한다.")
    void chatKeyTest() {
        // given
        ChatResponse response1 = chatService.createChatRoom(
                user1.getId(),
                new ChatRequest(List.of(user2.getId(), user3.getId()))
        );
        ChatResponse response2 = chatService.createChatRoom(
                user2.getId(),
                new ChatRequest(List.of(user3.getId(), user1.getId()))
        );

        // when
        ChatRoom chatRoom1 = chatRepository.findById(response1.getId()).orElseThrow();
        ChatRoom chatRoom2 = chatRepository.findById(response2.getId()).orElseThrow();

        // then
        assertThat(chatRoom1.getChatKey()).isEqualTo(chatRoom2.getChatKey());
        assertThat(chatRoom1.getId()).isEqualTo(chatRoom2.getId());

    }

}