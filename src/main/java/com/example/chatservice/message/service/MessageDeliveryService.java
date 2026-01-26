package com.example.chatservice.message.service;

import com.example.chatservice.common.ServerInfoProvider;
import com.example.chatservice.common.SessionManager;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.property.MessageForwardRetryProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(MessageForwardRetryProperty.class)
public class MessageDeliveryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SessionManager sessionManager;
    private final ServerInfoProvider serverInfoProvider;
    private final RestTemplate restTemplate;
    @Qualifier("retryScheduler")
    private final ScheduledExecutorService retryScheduler;
    private final MessageForwardRetryProperty retryProperty;

    public void deliverMessage(Long receiverId, Message message) {
        String redisKey = "user:" + receiverId;
        String targetServer = (String) redisTemplate.opsForValue().get(redisKey);

        log.info("Looking up user {} in Redis, found server: {}", receiverId, targetServer);

        if (targetServer == null) {
            log.warn("User {} is offline (not found in Redis)", receiverId);
            return;
        }

        String currentServer = serverInfoProvider.getServerAddress();
        log.info("Current server: {}, Target server: {}", currentServer, targetServer);

        if (targetServer.equals(currentServer)) {
            log.info("User {} is on the same server. Sending directly.", receiverId);

            Map<String, Object> messageData = createMessageData(message);
            sessionManager.sendToUser(receiverId, messageData);
            log.info("Message sent directly to user {}", receiverId);
        } else {
            log.info("User {} is on different server {}. Forwarding via HTTP.", receiverId, targetServer);
            forwardToOtherServer(targetServer, receiverId, message);
        }
    }

    private void forwardToOtherServer(String targetServer, Long receiverId, Message message) {
        forwardToOtherServerWithRetry(targetServer, receiverId, message, 0);
    }

    private void forwardToOtherServerWithRetry(String targetServer, Long receiverId, Message message, int retryCount) {
        // 서버 주소가 localhost 형식인지 확인하고 필요시 변환
        String serverAddress = normalizeServerAddress(targetServer);
        int maxRetries = retryProperty.getMaxRetries();

        try {
            Map<String, Object> request = createMessageData(message);
            request.put("receiverId", receiverId);

            String url = "http://" + serverAddress + "/internal/message";

            if (retryCount > 0) {
                log.warn("Retrying ({}/{}) to forward message to user {} via HTTP: {}",
                        retryCount, maxRetries, receiverId, url);
            } else {
                log.info("Forwarding message to user {} via HTTP: {}", receiverId, url);
            }

            restTemplate.postForObject(url, request, Void.class);
            log.info("Message forwarded successfully to {} for user {}", serverAddress, receiverId);

        } catch (Exception e) {
            retryCount++;
            final int nextRetryCount = retryCount; // 람다에서 사용하기 위해 final 변수로 복사
            log.error("Failed to forward message to {} for user {} (attempt {}/{}): {}",
                    serverAddress, receiverId, retryCount, maxRetries, e.getMessage());

            if (retryCount < maxRetries) {
                // ScheduledExecutorService를 사용하여 비동기로 재시도 (메인 스레드 블로킹 방지)
                long delayMs = retryProperty.getBaseDelayMs() * retryCount;
                retryScheduler.schedule(() -> {
                    forwardToOtherServerWithRetry(targetServer, receiverId, message, nextRetryCount);
                }, delayMs, TimeUnit.MILLISECONDS);
            } else {
                log.error("Failed to forward message after {} attempts to {} for user {}",
                        maxRetries, serverAddress, receiverId, e);
            }
        }
    }

    /**
     * 같은 서버에 있는 유저에게 로컬로 메시지 전송 (WebSocket)
     */
    public void deliverMessageLocally(Long receiverId, Message message) {
        Map<String, Object> messageData = createMessageData(message);
        sessionManager.sendToUser(receiverId, messageData);
        log.info("Message sent locally to user {}", receiverId);
    }

    /**
     * 다른 서버로 배치로 메시지 전송
     */
    public void deliverMessageBatch(String targetServer, List<Long> receiverIds, Message message) {
        deliverMessageBatchWithRetry(targetServer, receiverIds, message, 0);
    }

    private void deliverMessageBatchWithRetry(String targetServer, List<Long> receiverIds, Message message, int retryCount) {
        String serverAddress = normalizeServerAddress(targetServer);
        int maxRetries = retryProperty.getMaxRetries();

        try {
            Map<String, Object> request = createMessageData(message);
            request.put("receiverIds", receiverIds);  // 배열로 전송

            String url = "http://" + serverAddress + "/internal/message/batch";

            if (retryCount > 0) {
                log.warn("[BATCH] Retrying ({}/{}) to forward message to {} users via HTTP: {}",
                        retryCount, maxRetries, receiverIds.size(), url);
            } else {
                log.info("[BATCH] Forwarding message to {} users via HTTP: {}", receiverIds.size(), url);
            }

            restTemplate.postForObject(url, request, Void.class);
            log.info("[BATCH] Message forwarded successfully to {} for {} users", serverAddress, receiverIds.size());

        } catch (Exception e) {
            retryCount++;
            final int nextRetryCount = retryCount; // 람다에서 사용하기 위해 final 변수로 복사
            log.error("[BATCH] Failed to forward message to {} for {} users (attempt {}/{}): {}",
                    serverAddress, receiverIds.size(), retryCount, maxRetries, e.getMessage());

            if (retryCount < maxRetries) {
                // ScheduledExecutorService를 사용하여 비동기로 재시도 (메인 스레드 블로킹 방지)
                long delayMs = retryProperty.getBaseDelayMs() * retryCount;
                retryScheduler.schedule(() -> {
                    deliverMessageBatchWithRetry(targetServer, receiverIds, message, nextRetryCount);
                }, delayMs, TimeUnit.MILLISECONDS);
            } else {
                log.error("[BATCH] Failed to forward message after {} attempts to {} for {} users",
                        maxRetries, serverAddress, receiverIds.size(), e);
            }
        }
    }

    private Map<String, Object> createMessageData(Message message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", message.getId());
        messageData.put("senderId", message.getSenderId());
        messageData.put("content", message.getMessage());
        messageData.put("chatRoomId", message.getChatRoomId());
        messageData.put("sentAt", message.getCreatedAt());
        return messageData;
    }

    private String normalizeServerAddress(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return serverAddress;
        }

        if (serverAddress.startsWith("127.0.0.1:")) {
            return "localhost" + serverAddress.substring(9); // "127.0.0.1" 제거하고 "localhost" 추가
        }

        return serverAddress;
    }
}
