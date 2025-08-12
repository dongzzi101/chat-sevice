package com.example.chatservice.chat.sercivce;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

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
}
