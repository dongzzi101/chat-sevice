package com.example.chatservice.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "User not found"),
    CHAT_ROOM_NOT_FOUND("CHAT_ROOM_NOT_FOUND", HttpStatus.NOT_FOUND, "Chat room not found"),
    MESSAGE_NOT_FOUND("MESSAGE_NOT_FOUND", HttpStatus.NOT_FOUND, "Message not found"),
    USER_NOT_JOINED("USER_NOT_JOINED", HttpStatus.FORBIDDEN, "User is not joined to the chat room"),
    INVALID_REQUEST("INVALID_REQUEST", HttpStatus.BAD_REQUEST, "Invalid request"),
    INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    USER_ALREADY_JOIEDD("USER_ALREADY_JOINED", HttpStatus.BAD_REQUEST, "User is already joined"),
    ;

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;
}
