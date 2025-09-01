package com.example.chatservice.chat.sercivce;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;

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

    // TODO Q1 : userchat 매핑엔티티를 만들었는데 userchat service를 만들어서 해야하는지 기존 chat service, user service 이용하는 건지
    public void joinChatRoom(Long chatId, String username) {

        User user = userRepository.findByUsername(username);
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
    public void leaveChatRoom(Long chatId, String username) {
        User user = userRepository.findByUsername(username);
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));

        userChatRepository.deleteByUserAndChat(user, chat);
    }

    @Transactional
    public void sendMessage(Long chatId, MessageRequest messageRequest, String username) {
        User user = userRepository.findByUsername(username);
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new RuntimeException("채팅방 없음"));


//        Message.builder()


    }
}
