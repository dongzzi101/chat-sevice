package com.example.chatservice.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionManager {

    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<Long, Object> sessionLocks = new ConcurrentHashMap<>();  // WebSocket 전송 동기화용

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ServerInfoProvider serverInfoProvider;

    public void addSession(Long userId, WebSocketSession session) {
        try {
            userSessions.put(userId, session);
            sessionLocks.putIfAbsent(userId, new Object());  // 동기화용 lock 생성

            // Redis에 저장: user:{userId} -> serverAddress
            String serverAddress = serverInfoProvider.getServerAddress();
            try {
                redisTemplate.opsForValue().set("user:" + userId, serverAddress);
            } catch (Exception e) {
                log.error("Failed to save user {} to Redis, but session is registered locally. Error: {}", 
                        userId, e.getMessage());
                // Redis 실패해도 로컬 세션은 유지 (서버 간 통신만 실패)
            }

            log.info("User {} connected to server {}. Total sessions: {}",
                    userId, serverAddress, userSessions.size());
        } catch (Exception e) {
            log.error("Failed to add session for user {}", userId, e);
            throw e;
        }
    }

    public void removeSession(Long userId) {
        userSessions.remove(userId);
        sessionLocks.remove(userId);  // lock도 함께 제거
        redisTemplate.delete("user:" + userId);

        log.info("User {} disconnected. Total sessions: {}", userId, userSessions.size());
    }

    public void sendToUser(Long userId, Object message) {
        WebSocketSession session = userSessions.get(userId);
        Object lock = sessionLocks.get(userId);

        if (session != null && session.isOpen() && lock != null) {
            // WebSocket sendMessage()는 thread-safe하지 않으므로 동기화 필요
            synchronized (lock) {
                try {
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(jsonMessage));
                    log.info("Message sent to user {}: {}", userId, jsonMessage);
                } catch (IOException e) {
                    log.error("Failed to send message to user {}", userId, e);
                }
            }
        } else {
            log.warn("User {} session not found or closed", userId);
        }
    }

    public boolean isUserConnected(Long userId) {
        return userSessions.containsKey(userId);
    }

    public int getSessionCount() {
        return userSessions.size();
    }


//    @PreDestroy
//    public void gracefulStop() throws IOException, InterruptedException {
//        int count = 0;
//        for (WebSocketSession session : sessions) {
//            session.close();
//            count++;
//            if (count == 1000) {
//                count = 0;
//                Thread.sleep(3000);
//            }
//        }
//    }

}
