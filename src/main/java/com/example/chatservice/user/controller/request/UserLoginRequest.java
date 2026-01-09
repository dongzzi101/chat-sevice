package com.example.chatservice.user.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserLoginRequest {

    @NotBlank(message = "username은 필수입니다.")
    private final String username;

}
