package com.example.chatservice.chat.sercivce;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
