package com.example.chatservice.exception;

public class ChatRoomNotFoundException extends BusinessException {
    public ChatRoomNotFoundException(Long chatRoomId) {
        super(ErrorCode.CHAT_ROOM_NOT_FOUND, "Chat room not found: " + chatRoomId);
    }
}

