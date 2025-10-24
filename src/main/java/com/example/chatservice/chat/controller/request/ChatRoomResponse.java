package com.example.chatservice.chat.controller.request;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatRoomResponse {

    private String lastMessage;
    private LocalDateTime lastMessageDateTime;

    @Builder
    public ChatRoomResponse(String lastMessage, LocalDateTime lastMessageDateTime) {
        this.lastMessage = lastMessage;
        this.lastMessageDateTime = lastMessageDateTime;
    }
}
