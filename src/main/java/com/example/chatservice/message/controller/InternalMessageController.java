package com.example.chatservice.message.controller;

import com.example.chatservice.common.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class InternalMessageController {

    private final SessionManager sessionManager;

    /**
     * 다른 서버에서 메시지를 전달받는 엔드포인트 (단일 유저)
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

    /**
     * 다른 서버에서 배치로 메시지를 전달받는 엔드포인트 (여러 유저)
     * 예: 8080 서버에서 8081 서버로 200명의 유저에게 메시지 전달
     */

    /// TODO  post 요청을 /internal/message 에서는 250번 보낼때 이걸써서 한번에 보내겟다는 뜻인가?
    @PostMapping("/internal/message/batch")
    public void receiveMessageBatch(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> receiverIdsRaw = (List<Integer>) request.get("receiverIds");
            List<Long> receiverIds = receiverIdsRaw.stream()
                    .map(Integer::longValue)
                    .toList();

            Long messageId = Long.valueOf(request.get("messageId").toString());
            Long senderId = Long.valueOf(request.get("senderId").toString());
            String content = request.get("content").toString();
            Long chatRoomId = Long.valueOf(request.get("chatRoomId").toString());
            Object sentAt = request.get("sentAt");

            log.info("[BATCH] Received message from other server for {} users: senderId={}, content={}",
                    receiverIds.size(), senderId, content);

            // 메시지 데이터 구성
            Map<String, Object> messageData = Map.of(
                    "messageId", messageId,
                    "senderId", senderId,
                    "content", content,
                    "chatRoomId", chatRoomId,
                    "sentAt", sentAt != null ? sentAt.toString() : ""
            );

            // 각 유저에게 WebSocket으로 전송
            int successCount = 0;
            for (Long receiverId : receiverIds) {
                try {
                    sessionManager.sendToUser(receiverId, messageData);
                    successCount++;
                } catch (Exception e) {
                    log.error("[BATCH] Failed to send message to user {}", receiverId, e);
                }
            }

            log.info("[BATCH] Message delivered to {}/{} users via WebSocket", successCount, receiverIds.size());

        } catch (Exception e) {
            log.error("[BATCH] Failed to process batch message from other server", e);
            throw new RuntimeException("Failed to process internal batch message", e);
        }
    }
}
