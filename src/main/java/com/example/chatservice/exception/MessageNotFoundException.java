package com.example.chatservice.exception;

public class MessageNotFoundException extends BusinessException {
    public MessageNotFoundException(Long messageId) {
        super(ErrorCode.MESSAGE_NOT_FOUND, "Message not found: " + messageId);
    }
}

