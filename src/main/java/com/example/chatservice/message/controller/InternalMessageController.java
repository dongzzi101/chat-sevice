package com.example.chatservice.message.controller;

import com.example.chatservice.common.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class InternalMessageController {

    private final SessionManager sessionManager;

    /**
     * 다른 서버에서 메시지를 전달받는 엔드포인트
     * 예: 8080 서버에서 8081 서버로 메시지 전달
     */
    @PostMapping("/internal/message")
    public void receiveMessageFromOtherServer(@RequestBody Map<String, Object> request) {
        try {
            Long receiverId = Long.valueOf(request.get("receiverId").toString());
            Long messageId = Long.valueOf(request.get("messageId").toString());
            Long senderId = Long.valueOf(request.get("senderId").toString());
            String content = request.get("content").toString();
            Long chatRoomId = Long.valueOf(request.get("chatRoomId").toString());
            Object sentAt = request.get("sentAt");

            log.info("Received message from other server: receiverId={}, senderId={}, content={}",
                    receiverId, senderId, content);

            // 메시지 데이터 구성
            Map<String, Object> messageData = Map.of(
                    "messageId", messageId,
                    "senderId", senderId,
                    "content", content,
                    "chatRoomId", chatRoomId,
                    "sentAt", sentAt != null ? sentAt.toString() : ""
            );

            // SessionManager를 통해 해당 유저에게 전송
            sessionManager.sendToUser(receiverId, messageData);
            
            log.info("Message delivered to user {} via WebSocket", receiverId);

        } catch (Exception e) {
            log.error("Failed to process message from other server", e);
            throw new RuntimeException("Failed to process internal message", e);
        }
    }
}
