package com.example.chatservice.message.service;

import com.example.chatservice.common.ServerInfoProvider;
import com.example.chatservice.common.SessionManager;
import com.example.chatservice.message.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDeliveryService {

    private final RedisTemplate<String, String> redisTemplate;
    private final SessionManager sessionManager;
    private final ServerInfoProvider serverInfoProvider;
    private final RestTemplate restTemplate;

    public void deliverMessage(Long receiverId, Message message) {
        String redisKey = "user:" + receiverId;
        String targetServer = redisTemplate.opsForValue().get(redisKey);

        log.info("Looking up user {} in Redis, found server: {}", receiverId, targetServer);

        if (targetServer == null) {
            log.warn("User {} is offline (not found in Redis)", receiverId);
            return;
        }

        String currentServer = serverInfoProvider.getServerAddress();
        log.info("Current server: {}, Target server: {}", currentServer, targetServer);

        if (targetServer.equals(currentServer)) {
            log.info("User {} is on the same server. Sending directly.", receiverId);

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", message.getId());
            messageData.put("senderId", message.getSender().getId());
            messageData.put("content", message.getMessage());
            messageData.put("chatRoomId", message.getChatRoom().getId());
            messageData.put("sentAt", message.getCreatedAt());

            sessionManager.sendToUser(receiverId, messageData);
            log.info("Message sent directly to user {}", receiverId);
        } else {
            log.info("User {} is on different server {}. Forwarding via HTTP.", receiverId, targetServer);
            forwardToOtherServer(targetServer, receiverId, message);
        }
    }

    private void forwardToOtherServer(String targetServer, Long receiverId, Message message) {
        // 서버 주소가 localhost 형식인지 확인하고 필요시 변환
        String serverAddress = normalizeServerAddress(targetServer);
        
        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;
        
        while (retryCount < maxRetries && !success) {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("receiverId", receiverId);
                request.put("messageId", message.getId());
                request.put("senderId", message.getSender().getId());
                request.put("content", message.getMessage());
                request.put("chatRoomId", message.getChatRoom().getId());
                request.put("sentAt", message.getCreatedAt());

                String url = "http://" + serverAddress + "/internal/message";
                
                if (retryCount > 0) {
                    log.warn("Retrying ({}/{}) to forward message to user {} via HTTP: {}", 
                            retryCount, maxRetries, receiverId, url);
                } else {
                    log.info("Forwarding message to user {} via HTTP: {}", receiverId, url);
                }

                restTemplate.postForObject(url, request, Void.class);
                log.info("Message forwarded successfully to {} for user {}", serverAddress, receiverId);
                success = true;

            } catch (Exception e) {
                retryCount++;
                log.error("Failed to forward message to {} for user {} (attempt {}/{}): {}", 
                        serverAddress, receiverId, retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        // 재시도 전 짧은 대기
                        Thread.sleep(100 * retryCount); // 100ms, 200ms, 300ms
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Failed to forward message after {} attempts to {} for user {}", 
                            maxRetries, serverAddress, receiverId, e);
                }
            }
        }
    }
    
    /**
     * 같은 서버에 있는 유저에게 로컬로 메시지 전송 (WebSocket)
     */
    public void deliverMessageLocally(Long receiverId, Message message) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("messageId", message.getId());
        messageData.put("senderId", message.getSender().getId());
        messageData.put("content", message.getMessage());
        messageData.put("chatRoomId", message.getChatRoom().getId());
        messageData.put("sentAt", message.getCreatedAt());

        sessionManager.sendToUser(receiverId, messageData);
        log.info("Message sent locally to user {}", receiverId);
    }

    /**
     * 다른 서버로 배치로 메시지 전송
     * @param targetServer 대상 서버 주소 (예: localhost:8081)
     * @param receiverIds 수신자 ID 목록
     * @param message 전송할 메시지
     */
    public void deliverMessageBatch(String targetServer, List<Long> receiverIds, Message message) {
        String serverAddress = normalizeServerAddress(targetServer);
        
        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;
        
        while (retryCount < maxRetries && !success) {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("receiverIds", receiverIds);  // 배열로 전송
                request.put("messageId", message.getId());
                request.put("senderId", message.getSender().getId());
                request.put("content", message.getMessage());
                request.put("chatRoomId", message.getChatRoom().getId());
                request.put("sentAt", message.getCreatedAt());

                String url = "http://" + serverAddress + "/internal/message/batch";
                
                if (retryCount > 0) {
                    log.warn("[BATCH] Retrying ({}/{}) to forward message to {} users via HTTP: {}", 
                            retryCount, maxRetries, receiverIds.size(), url);
                } else {
                    log.info("[BATCH] Forwarding message to {} users via HTTP: {}", receiverIds.size(), url);
                }

                restTemplate.postForObject(url, request, Void.class);
                log.info("[BATCH] Message forwarded successfully to {} for {} users", serverAddress, receiverIds.size());
                success = true;

            } catch (Exception e) {
                retryCount++;
                log.error("[BATCH] Failed to forward message to {} for {} users (attempt {}/{}): {}", 
                        serverAddress, receiverIds.size(), retryCount, maxRetries, e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(100 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("[BATCH] Failed to forward message after {} attempts to {} for {} users", 
                            maxRetries, serverAddress, receiverIds.size(), e);
                }
            }
        }
    }

    /**
     * 서버 주소를 정규화합니다.
     * 127.0.0.1:8080 -> localhost:8080로 변환하거나 그대로 유지
     */
    private String normalizeServerAddress(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            return serverAddress;
        }
        
        // 127.0.0.1을 localhost로 변환 (선택적)
        // 실제 환경에서는 IP 주소를 그대로 사용하는 것이 좋을 수 있음
        if (serverAddress.startsWith("127.0.0.1:")) {
            return "localhost" + serverAddress.substring(9); // "127.0.0.1" 제거하고 "localhost" 추가
        }
        
        return serverAddress;
    }
}
