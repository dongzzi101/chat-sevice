package com.example.chatservice.chat.controller.request;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.function.Predicate;

@Getter
public class ChatRoomResponse {

    private Long chatRoomId;
    private String lastMessage;
    private LocalDateTime lastMessageDateTime;
    private Long unreadCount;
    private Long lastMessageId;


    @Builder
    public ChatRoomResponse(Long chatRoomId, String lastMessage, LocalDateTime lastMessageDateTime, Long unreadCount, Long lastMessageId) {
        this.chatRoomId = chatRoomId;
        this.lastMessage = lastMessage;
        this.lastMessageDateTime = lastMessageDateTime;
        this.unreadCount = unreadCount;
        this.lastMessageId = lastMessageId;
    }
}
