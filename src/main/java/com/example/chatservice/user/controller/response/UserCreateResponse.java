package com.example.chatservice.user.controller.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserCreateResponse {

    private final Long userId;
    private final String username;

    @Builder
    public UserCreateResponse(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
