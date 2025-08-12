package com.example.chatservice.message.controller;

import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    //TODO userId -> jwt token 변경해서 가져오기
    @PostMapping("/api/messages/{userId}/{receiverId}")
    public void sendMessage(@RequestBody MessageRequest message, @PathVariable Long userId, @PathVariable Long receiverId) {
        messageService.sendMessage(message, userId, receiverId);
    }

    @GetMapping("/api/messages")
    public List<MessageResponse> getMessages() {
        return messageService.getMessages();
    }


}
