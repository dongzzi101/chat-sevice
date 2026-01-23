package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.reponse.ChatResponse;
import com.example.chatservice.chat.controller.reponse.ChatRoomResponse;
import com.example.chatservice.chat.service.ChatService;
import com.example.chatservice.common.response.ApiResponse;
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
    public ApiResponse<ChatResponse> createChatRoom(@CurrentUser UserPrincipal userPrincipal, @RequestBody ChatRequest chatRequest) {
        Long currentUserId = getCurrentUserId(userPrincipal);

/**
        // 95% 유저는 원래 service 사용
        // 5%  유저는 newChatService.createChatRoomV2();
        // throw new RuntimeException("bla bla error");
*/
        return ApiResponse.ok(chatService.createChatRoom(currentUserId, chatRequest));
    }

    // 내 채팅방 리스트 조회
    @GetMapping("/api/v1/chats")
    public ApiResponse<List<ChatRoomResponse>> getChatRooms(@CurrentUser UserPrincipal userPrincipal) {
        Long currentUserId = getCurrentUserId(userPrincipal);
        return ApiResponse.ok(chatService.getChatRooms(currentUserId));
    }

    // 채팅방 가입
    @PostMapping("/api/v1/chat/{chatId}")
    public ApiResponse<Void> joinChat(@CurrentUser UserPrincipal userPrincipal, @PathVariable Long chatId) {
        Long currentUserId = getCurrentUserId(userPrincipal);
        chatService.joinChatRoom(chatId, currentUserId);
        return ApiResponse.ok(null);
    }

    // 채팅방 나가기
    @DeleteMapping("/api/v1/chat/{chatRoomId}")
    public ApiResponse<Void> leaveChat(@CurrentUser UserPrincipal userPrincipal, @PathVariable Long chatRoomId) {
        Long currentUserId = getCurrentUserId(userPrincipal);
        chatService.leaveChatRoom(chatRoomId, currentUserId);
        return ApiResponse.ok(null);
    }

    private static Long getCurrentUserId(@CurrentUser UserPrincipal userPrincipal) {
        return userPrincipal.getId();
    }


}