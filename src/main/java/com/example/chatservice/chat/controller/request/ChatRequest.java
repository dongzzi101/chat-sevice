package com.example.chatservice.chat.controller.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatRequest {

    private List<Long> userIds;

    @Builder
    public ChatRequest(List<Long> userIds) {
        this.userIds = userIds;
    }
}
