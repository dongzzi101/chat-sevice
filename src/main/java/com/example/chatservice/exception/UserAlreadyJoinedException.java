package com.example.chatservice.exception;

public class UserAlreadyJoinedException extends BusinessException {

    public UserAlreadyJoinedException(Long userId, Long chatId) {
        super(ErrorCode.USER_ALREADY_JOIEDD, "User not found: " + chatId + "userId:" + userId);
    }
}
