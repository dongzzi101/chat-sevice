package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.sercivce.ChatService;
import com.example.chatservice.message.controller.request.MessageRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 채팅방 만들기
    @PostMapping("/api/v1/chat")
    public void createChatRoom(@RequestBody ChatRequest chatRequest) {
        chatService.createChatRoom(chatRequest);
    }

    // 채팅방리스트 조회
    @GetMapping("/api/v1/chats")
    public List<ChatResponse> getChatRooms() {
        List<ChatResponse> chatResponses = chatService.getChatRooms();
        return chatResponses;
    }

/*    // 채팅방 가입
    @PostMapping("/api/v1/chat/{chatId}")
    public void joinChat(@PathVariable Long chatId) {
        //TODO : username이 중복이라면?
        String username = (String) httpSession.getAttribute("username");
        chatService.joinChatRoom(chatId, username);
    }


    // 채팅방 나가기
    @DeleteMapping("/api/v1/chat/{chatId}")
    public void leaveChat(@PathVariable Long chatId) {
        String username = (String) httpSession.getAttribute("username");
        chatService.leaveChatRoom(chatId, username);
    }

    // 채팅방? 에서 메시지 전송
    @PostMapping("/api/v1/chats/{chatId}")
    public void sendMessage(@PathVariable Long chatId, @RequestBody MessageRequest messageRequest) {
        String username = (String) httpSession.getAttribute("username");
        chatService.sendMessage(chatId, messageRequest, username);
    }*/


    // 채팅방? 에서 메시지 조회
//    @GetMapping("/api/v1/chats/{chatId}")






}