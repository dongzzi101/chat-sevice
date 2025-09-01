package com.example.chatservice.user.controller;

import com.example.chatservice.user.controller.request.UserCreateRequest;
import com.example.chatservice.user.controller.request.UserLoginRequest;
import com.example.chatservice.user.controller.response.UserCreateResponse;
import com.example.chatservice.user.controller.response.UserLoginResponse;
import com.example.chatservice.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/v1/users")
    public UserCreateResponse createUser(@RequestBody UserCreateRequest createRequest) {
        UserCreateResponse createResponse = userService.createUser(createRequest);
        return createResponse;
    }

    @PostMapping("/api/v1/users/login")
    public UserLoginResponse loginUser(@RequestBody UserLoginRequest userLoginRequest) {
        UserLoginResponse userLoginResponse = userService.login(userLoginRequest);
        return userLoginResponse;
    }
}
//TODO 2 : jwt flow 정리
/*
  - 1트
  1. JWT provider -> token 발급
  2. interceptor -> accessToken 확인 해줌
  3. username 이 아닌 변하지 않는 UserId 로 값을 가져와야함
  4. Jwt claim 만들 때 UserId를 같이 넣어줌
  5. Q. 근데 UserId 를 넣어서 만들어도 되나? -> 순서 유추가 가능하고 다른 유저가 쓸 수 있지 않나?
        -> uuid or snowflake 사용하면 어차피 유추하기가 힘든가?
  6. 근데 userId or username 을 어떻게 Controller or service 에서 사용하지?
  7. HttpServletRequest request 를 파라미터로 받아서 가져옴
  8. 지저분해짐

  - 2트
  1~5. 1트 동일
  6. @CurrentUser 같은 커스텀 어노테이션과 HandlerMethodArgumentResolver 사용

 */