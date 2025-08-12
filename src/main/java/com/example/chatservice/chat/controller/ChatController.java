package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.sercivce.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/api/v1/chat")
    public void createChatRoom(@RequestBody ChatRequest chatRequest) {
        chatService.createChatRoom(chatRequest);
    }

    @GetMapping("/api/v1/chats")
    public List<ChatResponse> getChatRooms() {
        List<ChatResponse> chatResponses = chatService.getChatRooms();
        return chatResponses;
    }




}
