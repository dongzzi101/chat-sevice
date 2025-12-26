package com.example.chatservice.exception;

public class UserNotJoinedException extends BusinessException {
    public UserNotJoinedException(Long chatRoomId, Long userId) {
        super(ErrorCode.USER_NOT_JOINED,
                "User %d is not joined to chat room %d".formatted(userId, chatRoomId));
    }
}
