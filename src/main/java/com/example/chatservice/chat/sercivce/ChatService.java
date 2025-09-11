package com.example.chatservice.chat.sercivce;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;

    @Transactional
    public void createChatRoom(ChatRequest chatRequest) {

        Chat chat = Chat.builder()
                .name(chatRequest.getName())
                .build();

        chatRepository.save(chat);
    }


    public List<ChatResponse> getChatRooms() {
        List<ChatResponse> chatResponses = new ArrayList<>();
        List<Chat> chats = chatRepository.findAll();

        for (Chat chat : chats) {
            ChatResponse chatResponse = new ChatResponse(chat.getName());
            chatResponses.add(chatResponse);
        }

        return chatResponses;
    }

    public void joinChatRoom(Long chatId, Long userId) {

        User user = userRepository.findById(userId).orElseThrow();
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));
        // 코드의 가독성을 좋게하기위해 예외케이스는 빨리빨리 던져버린다.
        // 이렇게 던진 에러(익셉션)을 따로 핸들링 해줘야하는가? 아닌가

        // 계좌의 잔액이 부족합니다
        // new 계좌잔액부족Exception();

        UserChat userChat = UserChat
                .builder()
                .user(user)
                .chat(chat)
                .build();

        userChatRepository.save(userChat);
    }

    // @ExceptionHandler(RuntimeException.class)
    // public String test(){}

    @Transactional
    public void leaveChatRoom(Long chatId, Long currentUserId) {
        User user = userRepository.findById(currentUserId).orElseThrow();
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));

        userChatRepository.deleteByUserAndChat(user, chat);
    }

    @Transactional
    public void sendMessage(Long chatId, MessageRequest messageRequest, Long currentUserId) {
        User user = userRepository.findById(currentUserId).orElseThrow();
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));


//        Message.builder()


    }
}
