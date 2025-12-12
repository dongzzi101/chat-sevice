package com.example.chatservice.chat.sercivce;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatType;
import com.example.chatservice.chat.entity.ReadStatus;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ReadStatusRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.fixture.TestEntityFactory;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.repository.Repository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.chatservice.fixture.TestEntityFactory.createChatRoom;
import static com.example.chatservice.fixture.TestEntityFactory.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

 // 모킹하기
 // 모킹안하기
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserChatRepository userChatRepository;

    @Mock
    private ReadStatusRepository readStatusRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("새로운 DIRECT 채팅방이 성공적으로 생성된다")
    void createDirectChatRoom() {
        /// given
        Long currentUserId = 1L;
        Long otherUserId = 2L;
        ChatRequest chatRequest = new ChatRequest(new ArrayList<>(List.of(otherUserId)));

        User currentUser = createUser(1L, "user1");
        User otherUser = createUser(2L, "user2");

        // 기존 채팅방이 없음을 가정
//        when(chatRepository.findChatRoomByChatKey(anyString())).thenReturn(Optional.empty());
        BDDMockito.given(chatRepository.findChatRoomByChatKey(anyString()))
                .willReturn(Optional.empty());

        //        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        BDDMockito.given(userRepository.findById(1L)).willReturn(Optional.of(currentUser));
        //        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        BDDMockito.given(userRepository.findById(2L)).willReturn(Optional.of(currentUser));

        // save 메서드가 호출될 때 전달된 ChatRoom에 ID를 설정하고 반환
//        when(chatRepository.save(any(ChatRoom.class))).thenAnswer(invocation ->
//                TestEntityFactory.setEntityId(invocation.getArgument(0), 100L)
//        );

        BDDMockito.given(chatRepository.save(any(ChatRoom.class)))
                .willAnswer(invocation ->
                        TestEntityFactory.setEntityId(invocation.getArgument(0), 100L)
                );


        /// when
        ChatResponse response = chatService.createChatRoom(currentUserId, chatRequest);

        /// then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getType()).isEqualTo(ChatType.DIRECT);

        // ChatRoom이 저장되었는지 확인
        ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        // 캡처(capture) = mock 호출 시 전달된 실제 인자값을 잡아서 꺼내오는 것
//        ArgumentCaptor = “인자를 저장할 그릇(도구)”
//        capture() = “그 그릇에 실제 인자를 담는 행동”


        verify(chatRepository, times(1)).save(chatRoomCaptor.capture());
        ChatRoom capturedChatRoom = chatRoomCaptor.getValue();
        assertThat(capturedChatRoom.getType()).isEqualTo(ChatType.DIRECT);
        assertThat(capturedChatRoom.getChatKey()).isEqualTo("1_2");

        // UserChat이 2번 저장되었는지 확인 (currentUser, otherUser)
        verify(userChatRepository, times(2)).save(any(UserChat.class));

        // ReadStatus가 2번 저장되었는지 확인
        verify(readStatusRepository, times(2)).save(any(ReadStatus.class));
    }

    @Test
    @DisplayName("새로운 GROUP 채팅방이 성공적으로 생성된다")
    void createGroupChatRoom() {
        // given
        Long currentUserId = 1L;
        List<Long> otherUserIds = Arrays.asList(2L, 3L);
        ChatRequest chatRequest = new ChatRequest(new ArrayList<>(otherUserIds));

        User user1 = createUser(1L, "user1");
        User user2 = createUser(2L, "user2");
        User user3 = createUser(3L, "user3");

        // 기존 채팅방이 없음을 가정
        when(chatRepository.findChatRoomByChatKey(anyString())).thenReturn(Optional.empty());

//        BDDMockito.given(chatRepository.findChatRoomByChatKey(anyString()))
//                        .willReturn(Optional.empty());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // save 메서드가 호출될 때 전달된 ChatRoom에 ID를 설정하고 반환
        when(chatRepository.save(any(ChatRoom.class))).thenAnswer(invocation ->
                TestEntityFactory.setEntityId(invocation.getArgument(0), 200L)
        );

        // when
        ChatResponse response = chatService.createChatRoom(currentUserId, chatRequest);

        // then
        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getType()).isEqualTo(ChatType.GROUP);

        // ChatRoom이 저장되었는지 확인
        ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRepository, times(1)).save(chatRoomCaptor.capture());
        ChatRoom capturedChatRoom = chatRoomCaptor.getValue();
        assertThat(capturedChatRoom.getType()).isEqualTo(ChatType.GROUP);
        assertThat(capturedChatRoom.getChatKey()).isEqualTo("1_2_3");

        // UserChat이 3번 저장되었는지 확인 (user1, user2, user3)
        verify(userChatRepository, times(3)).save(any(UserChat.class));

        // ReadStatus가 3번 저장되었는지 확인
        verify(readStatusRepository, times(3)).save(any(ReadStatus.class));
    }

    @Test
    @DisplayName("IM 채팅방(자기 자신과의 채팅)이 성공적으로 생성된다")
    void createImChatRoom() {
        // given
        Long currentUserId = 1L;
        List<Long> selfUserIds = Arrays.asList(1L); // 자기 자신
        ChatRequest chatRequest = new ChatRequest(new ArrayList<>(selfUserIds));

        User user1 = createUser(1L, "user1");

        // 기존 채팅방이 없음을 가정
        when(chatRepository.findChatRoomByChatKey(anyString())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        // save 메서드가 호출될 때 전달된 ChatRoom에 ID를 설정하고 반환
        when(chatRepository.save(any(ChatRoom.class))).thenAnswer(invocation ->
                TestEntityFactory.setEntityId(invocation.getArgument(0), 300L)
        );

        // when
        ChatResponse response = chatService.createChatRoom(currentUserId, chatRequest);

        // then
        assertThat(response.getId()).isEqualTo(300L);
        assertThat(response.getType()).isEqualTo(ChatType.IM);

        // ChatRoom이 저장되었는지 확인
        ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRepository, times(1)).save(chatRoomCaptor.capture());
        ChatRoom capturedChatRoom = chatRoomCaptor.getValue();
        assertThat(capturedChatRoom.getType()).isEqualTo(ChatType.IM);
        assertThat(capturedChatRoom.getChatKey()).isEqualTo("1_1");

        // UserChat이 1번 저장되었는지 확인 (중복 제거되어 1명만)
        verify(userChatRepository, times(1)).save(any(UserChat.class));

        // ReadStatus가 1번 저장되었는지 확인
        verify(readStatusRepository, times(1)).save(any(ReadStatus.class));
    }

    @Test
    @DisplayName("기존 채팅방이 있으면 새로 생성하지 않고 기존 채팅방을 반환한다")
    void returnExistingChatRoom() {
        // given
        Long currentUserId = 1L;
        Long otherUserId = 2L;
        ChatRequest chatRequest = new ChatRequest(new ArrayList<>(Arrays.asList(otherUserId)));

        ChatRoom existingChatRoom = createChatRoom(100L, ChatType.DIRECT, "1_2");

        // 기존 채팅방이 있음을 가정
        when(chatRepository.findChatRoomByChatKey("1_2")).thenReturn(Optional.of(existingChatRoom));

        // when
        ChatResponse response = chatService.createChatRoom(currentUserId, chatRequest);

        // then
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getType()).isEqualTo(ChatType.DIRECT);

        // 새로운 ChatRoom이 저장되지 않았는지 확인
        verify(chatRepository, never()).save(any(ChatRoom.class));

        // UserChat도 저장되지 않았는지 확인
        verify(userChatRepository, never()).save(any(UserChat.class));

        // ReadStatus도 저장되지 않았는지 확인
        verify(readStatusRepository, never()).save(any(ReadStatus.class));
    }

    @Test
    @DisplayName("채팅방 생성 시 chatKey가 정렬된 userId로 생성된다")
    void createChatRoomWithSortedChatKey() {
        // given
        Long currentUserId = 3L;
        List<Long> otherUserIds = Arrays.asList(1L, 5L, 2L); // 정렬되지 않은 순서
        ChatRequest chatRequest = new ChatRequest(new ArrayList<>(otherUserIds));

        User user1 = createUser(1L, "user1");
        User user2 = createUser(2L, "user2");
        User user3 = createUser(3L, "user3");
        User user5 = createUser(5L, "user5");

        // 기존 채팅방이 없음을 가정
        when(chatRepository.findChatRoomByChatKey(anyString())).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user5));

        // save 메서드가 호출될 때 전달된 ChatRoom에 ID를 설정하고 반환
        when(chatRepository.save(any(ChatRoom.class))).thenAnswer(invocation ->
                TestEntityFactory.setEntityId(invocation.getArgument(0), 400L)
        );

        // when
        ChatResponse response = chatService.createChatRoom(currentUserId, chatRequest);

        // then
        // ChatRoom이 저장될 때 chatKey가 정렬된 순서로 생성되었는지 확인
        ArgumentCaptor<ChatRoom> chatRoomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRepository, times(1)).save(chatRoomCaptor.capture());
        ChatRoom capturedChatRoom = chatRoomCaptor.getValue();
        assertThat(capturedChatRoom.getChatKey()).isEqualTo("1_2_3_5"); // 정렬된 순서
    }
}

// datadog password
// d4Dd@hzVQCHLZJ8