package com.example.chatservice.message.service;

import com.example.chatservice.message.entity.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageDto {
    private Long messageId;
    private Long senderId;
    private Long chatRoomId;
    private String content;
    private LocalDateTime sentAt;

    public static MessageDto from(Message message) {
        return MessageDto.builder()
                .messageId(message.getId())
                .senderId(message.getSender().getId())
                .chatRoomId(message.getChatRoom().getId())
                .content(message.getMessage())
                .sentAt(message.getCreatedAt())
                .build();
    }
}
