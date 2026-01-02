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

    @GetMapping("/api/v1/messages/{chatRoomId}")
    public List<MessageResponse> getMessages(
            @CurrentUser UserPrincipal userPrincipal,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long lastReadMessageId,
            @RequestParam(defaultValue = "3") int before,
            @RequestParam(defaultValue = "3") int after
    ) {
        Long currentUserId = userPrincipal.getId();
        return messageService.getMessages(currentUserId, chatRoomId, lastReadMessageId, before, after);
    }

    @PostMapping("/api/v1/messages/{chatRoomId}/read")
    public void readMessage(
            @CurrentUser UserPrincipal userPrincipal,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long messageId
    ) {
        Long currentUserId = userPrincipal.getId();
        messageService.markMessagesAsRead(currentUserId, chatRoomId, messageId);
    }


}
