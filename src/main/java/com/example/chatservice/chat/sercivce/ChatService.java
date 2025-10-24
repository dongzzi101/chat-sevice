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
import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ChatResponse createChatRoom(Long currentUserId, ChatRequest chatRequest) {

        List<Long> userIds = chatRequest.getUserIds();
        userIds.add(currentUserId);

        ChatRoom chatRoom;

        // TODO:FLOW - 3. 채팅방이 있는지 확인
        ChatRoom existChatRoom = findExistChatRoom(userIds);

        // TODO:FLOW - 3.1. 있는 경우 기존 채팅방
        if (existChatRoom != null) {
            chatRoom = existChatRoom;

        } else {

            // TODO:FLOW - 3.2. 채팅방 없는 경우 새로운 채팅방을 생성

            String chatKey = createChatKey(userIds);

            chatRoom = ChatRoom.builder()
                    .type(userIds.size() == 2 ? ChatType.DIRECT : ChatType.GROUP)
                    .chatKey(chatKey)
                    .build();

            chatRepository.save(chatRoom);

            for (Long userId : userIds) {
                UserChat userChat = UserChat.builder()
                        .chatRoom(chatRoom)
                        .user(userRepository.findById(userId).get())
                        .build();
                userChatRepository.save(userChat);

                // TODO:FLOW - 4.채팅방 생성 시 read_status 생성
                ReadStatus readStatus = ReadStatus.builder()
                        .user(userRepository.findById(userId).get())
                        .chatRoom(chatRoom)
                        .lastReadMessage(null)
                        .build();
                readStatusRepository.save(readStatus);
            }

        }

        return new ChatResponse(chatRoom.getId(), chatRoom.getType());

    }

    private ChatRoom findExistChatRoom(List<Long> userIds) {
        String chatKey = findChatKey(userIds);
        ChatRoom chatRoom = chatRepository.findChatRoomByChatKey(chatKey).orElse(null);
        return chatRoom;

//        if (userIds.size() == 2) {
//            return userChatRepository.findDirectChatRoomByUserIds(userIds).orElse(null);
//        } else {
//            return userChatRepository.findGroupChatRoomByUserIds(userIds, userIds.size()).orElse(null);
//        }
    }

    // chat key 생성해주는 역할
    private String createChatKey(List<Long> userIds) {
        userIds.sort(Comparator.naturalOrder());
        return userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("_"));
    }

    // chat key를 찾는 역할?
    private String findChatKey(List<Long> userIds) {
        userIds.sort(Comparator.naturalOrder());
        return userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("_"));
    }


    public List<ChatRoomResponse> getChatRooms(Long currentUserId) {

        List<ChatRoomResponse> chatroomResponses = new ArrayList<>();

        List<UserChat> userChatList = userChatRepository.findByUserId(currentUserId);


        for (UserChat userChat : userChatList) {

            ChatRoom chatRoom = userChat.getChatRoom();

            Message lastMessage = messageRepository
                    // TODO : 여기부분 공부하기
                    .findTopByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId())
                    .orElse(null);

            ChatRoomResponse response = ChatRoomResponse.builder()
                    .lastMessage(lastMessage.getMessage())
                    .lastMessageDateTime(lastMessage.getCreatedAt())
                    .build();

            chatroomResponses.add(response);
        }

        // TODO : 여기부분 공부하기
        chatroomResponses.sort((a, b) -> {
            if (a.getLastMessageDateTime() == null) return 1;
            if (b.getLastMessageDateTime() == null) return -1;
            return b.getLastMessageDateTime().compareTo(a.getLastMessageDateTime());
        });

        return chatroomResponses;
    }

    public void joinChatRoom(Long chatId, Long userId) {

        User user = userRepository.findById(userId).orElseThrow();
        ChatRoom chatRoom = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));
        // 코드의 가독성을 좋게하기위해 예외케이스는 빨리빨리 던져버린다.
        // 이렇게 던진 에러(익셉션)을 따로 핸들링 해줘야하는가? 아닌가

        // 계좌의 잔액이 부족합니다
        // new 계좌잔액부족Exception();

        UserChat userChat = UserChat
                .builder()
                .user(user)
                .chatRoom(chatRoom)
                .build();

        userChatRepository.save(userChat);
    }

    // @ExceptionHandler(RuntimeException.class)
    // public String test(){}

    @Transactional
    public void leaveChatRoom(Long chatId, Long currentUserId) {
        User user = userRepository.findById(currentUserId).orElseThrow();
        ChatRoom chatRoom = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));

        userChatRepository.deleteByUserAndChatRoom(user, chatRoom);
    }

    @Transactional
    public void sendMessage(Long chatId, MessageRequest messageRequest, Long currentUserId) {
        User user = userRepository.findById(currentUserId).orElseThrow();
        ChatRoom chatRoom = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));

//        Message.builder()


    }
}
