package com.example.chatservice.message.controller;

import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.service.MessageService;
import com.example.chatservice.user.CurrentUser;
import com.example.chatservice.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/api/v1/messages/{receiverId}")
    public void sendMessage(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestBody MessageRequest message,
            @PathVariable Long receiverId) {

        Long senderUserId = userPrincipal.getId();

        messageService.sendMessage(message, senderUserId, receiverId);
    }

    // 나에게 온 메시지를 들고오기 -> TODO readID? 확인하는 로직 넣기
    @GetMapping("/api/v1/messages/{userId}")
    public List<MessageResponse> getMessages(@PathVariable Long userId) {
        return messageService.getMessages(userId);
    }

}
