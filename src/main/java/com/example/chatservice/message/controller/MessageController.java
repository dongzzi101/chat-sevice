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

    // TODO : 메세지 전송 flow
    /*
     * 1. 메시지를 개인 or 단체를 보냄 (한 api 사용)
     * 2. 어차피 개인방도 챗룸을 가진다.
     */


    // TODO : /api/v1/messages/{receiverId} ??
    /**
     * 1. receiverId 를 받아야하나?
     * 2. 챗 Id에 보낸다면? -> 개인은 그냥 받는 느낌, 단체는 다 받는 느낌
     *
     */
    @PostMapping("/api/v1/messages/{chatRoomId}")
    public void sendMessage(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestBody MessageRequest message,
            @PathVariable Long chatRoomId) {

        Long senderUserId = userPrincipal.getId();

        messageService.sendMessage(message, senderUserId, chatRoomId);
    }

    // 나에게 온 메시지를 들고오기 -> TODO readID? 확인하는 로직 넣기
    @GetMapping("/api/v1/messages/{userId}")
    public List<MessageResponse> getMessages(@PathVariable Long userId) {
        return messageService.getMessages(userId);
    }

}
