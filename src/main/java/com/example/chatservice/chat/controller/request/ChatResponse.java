package com.example.chatservice.chat.controller.request;

import com.example.chatservice.chat.entity.ChatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ChatResponse {

    private Long id;
    private ChatType type;

    @Builder
    public ChatResponse(Long id, ChatType type) {
        this.id = id;
        this.type = type;
    }
}
