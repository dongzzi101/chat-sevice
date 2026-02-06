package com.example.chatservice.user.service;

import com.example.chatservice.exception.UserNotFoundException;
import com.example.chatservice.jwt.JWTProvider;
import com.example.chatservice.user.controller.request.UserCreateRequest;
import com.example.chatservice.user.controller.request.UserLoginRequest;
import com.example.chatservice.user.controller.response.UserCreateResponse;
import com.example.chatservice.user.controller.response.UserLoginResponse;
import com.example.chatservice.user.controller.response.UserResponse;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserService { // UserServiceCGlibSpringProxyTx

    private final UserRepository userRepository;
//    private final RedisTemplate<Object, Object> redisTemplate;

//    public void test(){
//        List<User> users = ...;
//
//        List<User> otherUsers = ...;
//        if(otherUsers) {
//            // condition == true
//            users.add(otherUser[0]);
//            users.add(otherUser[1]);
//        }
//
//        // state -> changed
//        List<Integer> numbers = List.of(1, 2, 3);
//        Stream<Integer> stream = numbers.stream()
//                .filter(n -> n % 2 == 0);
//
//
//        // immutable
//        List<User> newUsers = users.map(...filter...);
//    }

    @Transactional
    public UserCreateResponse createUser(UserCreateRequest createRequest) {

        User user = User.builder()
                .username(createRequest.getUsername())
                .build();

        userRepository.save(user);

        String accessToken = JWTProvider.generateToken(user.getId(), user.getUsername());

        return UserCreateResponse
                .builder()
                .userId(user.getId())
                .username(user.getUsername())
                .accessToken(accessToken)
                .build();
    }

    @Transactional
    public UserLoginResponse login(UserLoginRequest userLoginRequest) {
        User user = userRepository.findByUsername(userLoginRequest.getUsername());

        if (user == null) {
            throw new UserNotFoundException(null);
        }

        String accessToken = JWTProvider.generateToken(user.getId(), user.getUsername());

        return UserLoginResponse.builder()
                .username(user.getUsername())
                .accessToken(accessToken)
                .build();
    }

    public List<UserResponse> getUsers() {
        List<User> users = userRepository.findAll();

        List<UserResponse> userResponses = new ArrayList<>();

        for (User user : users) {
            UserResponse userResponse = new UserResponse(user.getId(), user.getUsername());
            userResponses.add(userResponse);
        }

        return userResponses;
    }
}
