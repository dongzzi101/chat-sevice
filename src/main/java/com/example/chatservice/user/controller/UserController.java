package com.example.chatservice.user.controller;

import com.example.chatservice.user.controller.request.UserCreateRequest;
import com.example.chatservice.user.controller.response.UserCreateResponse;
import com.example.chatservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/v1/users")
    public UserCreateResponse createUser(@RequestBody UserCreateRequest createRequest) {
        UserCreateResponse createResponse = userService.createUser(createRequest);
        return createResponse;
    }
}
