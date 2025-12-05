package com.example.chatservice.chat.controller.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatRequest {

    private List<Long> userIds;

    public ChatRequest(List<Long> userIds) {
        this.userIds = userIds;
    }
}
