package com.example.chatservice.message.controller;

import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.service.MessageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    //TODO userId -> jwt token 변경해서 가져오기
    @PostMapping("/api/v1/messages/{receiverId}")
    public void sendMessage(@RequestBody MessageRequest message, HttpSession httpSession, @PathVariable Long receiverId) {
        String senderUsername = (String) httpSession.getAttribute("username");

        System.out.println("=== 메시지 전송 요청 ===");
        System.out.println("Session ID: " + httpSession.getId());
        System.out.println("Message: " + message.getMessage());
        System.out.println("Receiver ID: " + receiverId);

        messageService.sendMessage(message, senderUsername, receiverId);
    }

    // 나에게 온 메시지를 들고오기 -> TODO readID? 확인하는 로직 넣기
    @GetMapping("/api/v1/messages/{userId}")
    public List<MessageResponse> getMessages(@PathVariable Long userId) {
        return messageService.getMessages(userId);
    }


}
