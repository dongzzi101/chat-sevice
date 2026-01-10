package com.example.chatservice.user.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserCreateRequest {

    @NotBlank(message = "username은 필수입니다.")
    private String username;

    public UserCreateRequest(String username) {
        this.username = username;
    }
}
