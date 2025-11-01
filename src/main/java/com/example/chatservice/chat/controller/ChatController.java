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

    // TODO 3 : 채팅방 flow

    /**
     * 기존 메시지를 보낼 때 보내는 쪽에서 senderId를 헤더에 넣고 받는쪽에 receiverId를 넣어서 받았음
     * e.g) /api/v1/messages/{receiverId}

     * 근데 이렇게 하는 것보다 그룹 or 개인 상관없이 chatRoomId(채티방 Id)를 만들어서
     * 거기가다 보내주는 게 나은 듯
     * e.g) /api/v1/messages/{chatRoomId}

     * 여기서 문제는 맨 처음 메시지를 chatRoomId가 없는데 어떻게 보내야하지?
     * 1. userId를 임의적으로 받아서 만든다 (하나만 받으면 개인, 여러개 받으면 단체 채팅방)
     */


    /**
     * 단체 메시지는 채팅방을 만들면서 참가자들 초대하는 로직이 이해가 됨
     * 근데 1대1일 메시지에서는 채팅방 생성을 따로 하는 게 아니고
     * 메시지를 보냄과 동시에 채팅방이 만들어 지는 건가?
     */

    // 채팅방 만들기
    // TODO:FLOW - 2.사용자A 가 친구A한테 메시지를 보낸다고 가정
    @PostMapping("/api/v1/chat")
    public ChatResponse createChatRoom(@CurrentUser UserPrincipal userPrincipal, @RequestBody ChatRequest chatRequest) {
        Long id = userPrincipal.getId();
        ChatResponse chatRoom = chatService.createChatRoom(id, chatRequest);
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

    // 채팅방? 에서 메시지 전송
    @PostMapping("/api/v1/chats/{chatId}")
    public void sendMessage(@CurrentUser UserPrincipal userPrincipal, @PathVariable Long chatId, @RequestBody MessageRequest messageRequest) {
        // httrequest or httpattributye . get("userId")
        Long currentUserId = userPrincipal.getId();
        chatService.sendMessage(chatId, messageRequest, currentUserId);
    }



}