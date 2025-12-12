package com.example.chatservice.user.controller.response;

import lombok.Builder;
import lombok.Getter;

@Getter
public class UserCreateResponse {

    private final Long userId;
    private final String username;
    private final String accessToken;

    @Builder
    public UserCreateResponse(Long userId, String username, String accessToken) {
        this.userId = userId;
        this.username = username;
        this.accessToken = accessToken;
    }
}
