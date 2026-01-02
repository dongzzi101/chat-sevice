package com.example.chatservice.user.service;

import com.example.chatservice.user.controller.request.UserCreateRequest;
import com.example.chatservice.user.controller.request.UserLoginRequest;
import com.example.chatservice.user.controller.response.UserCreateResponse;
import com.example.chatservice.user.controller.response.UserLoginResponse;
import com.example.chatservice.user.controller.response.UserResponse;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {
    /**
     * 1. 유저생성
     * 1-1. 생성과 동시에 token 발급
     * ------------------------
     * 2. 로그인
     * 2-1. 로그인 하면 token 발급
     * ------------------------
     * 3. 유저 목록
     */

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Test
    @DisplayName("유저 회원가입하면 토큰도 자동 발급한다.")
    void createUser() {
        // given & when
        UserCreateResponse userCreateResponse = userService.createUser(new UserCreateRequest("userA"));

        // then
        assertThat(userCreateResponse.getUserId()).isNotNull();
        assertThat(userCreateResponse.getUsername()).isEqualTo("userA");
        assertThat(userCreateResponse.getAccessToken()).isNotNull();
    }

    @Test
    @DisplayName("유저 로그인시 토큰도 자동 발급한다.")
    void login() {
        // given
        User user1 = User.builder()
                .username("userA")
                .build();
        userRepository.save(user1);

        // when
        UserLoginResponse userLoginResponse = userService.login(new UserLoginRequest(user1.getUsername()));

        // then
        assertThat(userLoginResponse.getUsername()).isEqualTo(user1.getUsername());
        assertThat(userLoginResponse.getAccessToken()).isNotNull();
    }

    @Test
    @DisplayName("유저 목록을 불러온다.")
    void getUsers() {
        // given
        User userA = User.builder()
                .username("userA")
                .build();

        User userB = User.builder()
                .username("userB")
                .build();

        User userC = User.builder()
                .username("userC")
                .build();

        userRepository.save(userA);
        userRepository.save(userB);
        userRepository.save(userC);

        // when
        List<UserResponse> users = userService.getUsers();

        // then
        assertThat(users.size()).isEqualTo(3);
    }
}