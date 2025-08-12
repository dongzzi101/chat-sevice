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
    @PostMapping("/api/v1/messages/{userId}/{receiverId}")
    public void sendMessage(@RequestBody MessageRequest message, @PathVariable Long userId, @PathVariable Long receiverId) {
        messageService.sendMessage(message, userId, receiverId);
    }

    // 나에게 온 메시지를 들고오기 -> TODO readID? 확인하는 로직 넣기
    @GetMapping("/api/v1/messages/{userId}")
    public List<MessageResponse> getMessages(@PathVariable Long userId) {
        return messageService.getMessages(userId);
    }


}
