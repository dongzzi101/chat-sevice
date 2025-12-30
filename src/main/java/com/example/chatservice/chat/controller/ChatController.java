package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.controller.request.ChatRoomResponse;
import com.example.chatservice.chat.sercivce.ChatService;
import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.user.CurrentUser;
import com.example.chatservice.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // 채팅방 만들기
    @PostMapping("/api/v1/chat")
    public ChatResponse createChatRoom(@CurrentUser UserPrincipal userPrincipal, @RequestBody ChatRequest chatRequest) {
        Long id = userPrincipal.getId();
        // 95% 유저는 원래 service 사용
        ChatResponse chatRoom = chatService.createChatRoom(id, chatRequest);
        // 5%  유저는 newChatService.createChatRoomV2();

        // throw new RuntimeException("bla bla error");

        return chatRoom;
    }

    // 내 채팅방 리스트 조회
    @GetMapping("/api/v1/chats")
    // TODO:FLOW 내 채팅방 리스트 조회(GET /api/v1/chats)
    public List<ChatRoomResponse> getChatRooms(@CurrentUser UserPrincipal userPrincipal) {
        Long currentUserId = userPrincipal.getId();
        List<ChatRoomResponse> ChatRoomResponse = chatService.getChatRooms(currentUserId);
        return ChatRoomResponse;
    }

    // 채팅방 가입
    @PostMapping("/api/v1/chat/{chatId}")
    public void joinChat(@CurrentUser UserPrincipal userPrincipal, @PathVariable Long chatId) {
        Long currentUserId = userPrincipal.getId();
        chatService.joinChatRoom(chatId, currentUserId);
    }


    // 채팅방 나가기
    @DeleteMapping("/api/v1/chat/{chatRoomId}")
    public void leaveChat(@CurrentUser UserPrincipal userPrincipal, @PathVariable Long chatRoomId) {
        Long currentUserId = userPrincipal.getId();
        // TODO : update chat_key
        chatService.leaveChatRoom(chatRoomId, currentUserId);
    }



}