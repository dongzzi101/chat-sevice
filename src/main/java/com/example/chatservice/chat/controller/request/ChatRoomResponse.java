package com.example.chatservice.chat.controller.request;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.function.Predicate;

@Getter
public class ChatRoomResponse {

    private final Long chatRoomId;
    private final String lastMessage;
    private final LocalDateTime lastMessageDateTime;
    private final Long unreadCount;
    private final Long lastMessageId;


    @Builder
    public ChatRoomResponse(Long chatRoomId, String lastMessage, LocalDateTime lastMessageDateTime, Long unreadCount, Long lastMessageId) {
        this.chatRoomId = chatRoomId;
        this.lastMessage = lastMessage;
        this.lastMessageDateTime = lastMessageDateTime;
        this.unreadCount = unreadCount;
        this.lastMessageId = lastMessageId;
    }
}
