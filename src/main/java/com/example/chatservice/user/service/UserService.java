package com.example.chatservice.user.service;

import com.example.chatservice.user.controller.request.UserCreateRequest;
import com.example.chatservice.user.controller.response.UserCreateResponse;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserCreateResponse createUser(UserCreateRequest createRequest) {

        User user = User.builder()
                .username(createRequest.getUsername())
                .build();

        userRepository.save(user);

        return UserCreateResponse
                .builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }
}
