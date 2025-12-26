package com.example.chatservice.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;
    private final String path;
    private final Instant timestamp;

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message != null ? message : errorCode.getMessage())
                .path(path)
                .timestamp(Instant.now())
                .build();
    }
}

