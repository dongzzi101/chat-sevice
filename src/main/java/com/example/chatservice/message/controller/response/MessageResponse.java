package com.example.chatservice.message.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class MessageResponse {

    private Long senderId;
    private String message;

    @Builder
    public MessageResponse(Long senderId, String message) {
        this.senderId = senderId;
        this.message = message;
    }
}
