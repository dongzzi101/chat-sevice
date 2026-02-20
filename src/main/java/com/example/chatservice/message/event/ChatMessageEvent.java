package com.example.chatservice.message.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka로 전송되는 채팅 메시지 이벤트
 * 서버 간 메시지 전달을 위한 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

    private Long messageId;
    private Long senderId;
    private String content;
    private Long chatRoomId;
    private LocalDateTime createdAt;

    public Map<String, Object> toWebSocketPayload() {
        return Map.of(
            "messageId", messageId,
            "senderId", senderId,
            "content", content,
            "chatRoomId", chatRoomId,
            "createdAt", createdAt
        );
    }
}







