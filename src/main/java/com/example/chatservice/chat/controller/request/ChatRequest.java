package com.example.chatservice.chat.controller.request;

import lombok.Getter;

import java.util.List;

@Getter
public class ChatRequest {

//    private String name;
    private List<Long> userId;
}
