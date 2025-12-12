package com.example.chatservice.message.controller.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageResponse {

    private Long senderId;
    private String message;

}
