package com.example.chatservice.user.service;

import com.example.chatservice.user.controller.request.UserCreateRequest;
import com.example.chatservice.user.controller.response.UserCreateResponse;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("회원가입")
    void createUser() {
        //given
        String username = "username123";
        User newUser = User.builder()
                .username(username)
                .build();

        //when
        User savedUser = userRepository.save(newUser);

        //then
        assertThat(savedUser.getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("회원가입하면 토큰이 발급 됨")
    void getTokenAfterSignUp() {
        // given
        UserCreateRequest userCreateRequest = new UserCreateRequest("username123");

        // when
        UserCreateResponse userCreateResponse = userService.createUser(userCreateRequest);

        // then
        assertThat(userCreateResponse.getAccessToken()).isNotNull();
    }



}