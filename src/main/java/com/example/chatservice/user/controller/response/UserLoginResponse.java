package com.example.chatservice.user.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Getter
@ToString
public class UserLoginResponse {

    private String username;
    private String accessToken;

}
