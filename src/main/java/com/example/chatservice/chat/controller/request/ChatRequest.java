package com.example.chatservice.chat.controller.request;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatRequest {

    private List<Long> userIds;

}
