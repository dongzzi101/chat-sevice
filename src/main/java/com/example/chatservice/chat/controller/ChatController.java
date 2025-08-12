package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.sercivce.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/api/chat")
    public void createChatRoom(@RequestBody ChatRequest chatRequest) {
        chatService.createChatRoom(chatRequest);
    }


}
