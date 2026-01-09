package com.example.chatservice.chat.controller.reponse;

import com.example.chatservice.chat.entity.ChatType;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ChatResponse {

    private final Long id;
    private final ChatType type;

    @Builder
    public ChatResponse(Long id, ChatType type) {
        this.id = id;
        this.type = type;
    }
}
