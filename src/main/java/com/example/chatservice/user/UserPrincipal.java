package com.example.chatservice.user;

import lombok.Getter;

@Getter
public class UserPrincipal {

    private final Long id;
    private final String username;

    public UserPrincipal(Long id, String username) {
        this.id = id;
        this.username = username;
    }
}
