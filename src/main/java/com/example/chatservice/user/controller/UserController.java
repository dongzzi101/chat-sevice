package com.example.chatservice.user.controller;

import com.example.chatservice.common.response.ApiResponse;
import com.example.chatservice.user.controller.request.UserCreateRequest;
import com.example.chatservice.user.controller.request.UserLoginRequest;
import com.example.chatservice.user.controller.response.UserCreateResponse;
import com.example.chatservice.user.controller.response.UserLoginResponse;
import com.example.chatservice.user.controller.response.UserResponse;
import com.example.chatservice.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/v1/users")
    public ApiResponse<UserCreateResponse> createUser(@Valid @RequestBody UserCreateRequest createRequest) {
        return ApiResponse.ok(userService.createUser(createRequest));
    }

    @PostMapping("/api/v1/users/login")
    public ApiResponse<UserLoginResponse> loginUser(@Valid @RequestBody UserLoginRequest userLoginRequest) {
        return ApiResponse.ok(userService.login(userLoginRequest));
    }

    // TODO:FLOW - 1. 사용자 목록을 보여줌
    @GetMapping("/api/v1/users")
    public List<UserResponse> getUsers() {
        List<UserResponse> users = userService.getUsers();
        return users;
    }
}
// 추가로 이미지 업로드 (사진전송)


/*
user table
id
nickname
age
created_at
updated_at

posts table
id
title
content
created_at
thumbnail_image   (sfasdfsdaf-sadf9asdf-sadf-sadf0sadf-sdafsdaf)
user_id

s3

select p.title, p.content, p.created_at, u.nickname from posts as p join users as u on posts.user_id = u.id;
* */
















