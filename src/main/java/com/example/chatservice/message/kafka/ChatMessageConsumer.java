package com.example.chatservice.message.kafka;

import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.common.ServerInfoProvider;
import com.example.chatservice.common.SessionManager;
import com.example.chatservice.message.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Kafka Consumer
 * Kafka 토픽에서 메시지를 소비하여 WebSocket으로 전달
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageConsumer {

    private final UserChatRepository userChatRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SessionManager sessionManager;
    private final ServerInfoProvider serverInfoProvider;

    /**
     * Kafka 토픽에서 메시지를 소비
     */
    @KafkaListener(
        topics = "chat-messages",
        groupId = "chat-server-${server.port}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeMessage(ChatMessageEvent event, Acknowledgment ack) {
        try {
            log.info("[Kafka Consumer] Received message: messageId={}, chatRoomId={}, senderId={}", 
                    event.getMessageId(), event.getChatRoomId(), event.getSenderId());
            
            String myServer = serverInfoProvider.getServerAddress();
            
            // 1. 채팅방 참여자 조회
            List<UserChat> participants = userChatRepository
                .findActiveByChatRoomWithUser(event.getChatRoomId());
            
            if (participants.isEmpty()) {
                log.warn("[Kafka Consumer] No participants found for chatRoomId={}", event.getChatRoomId());
                ack.acknowledge();
                return;
            }
            
            // 2. Redis Pipeline으로 서버 정보 일괄 조회 (성능 최적화)
            List<String> userKeys = participants.stream()
                .map(uc -> "user:" + uc.getUser().getId())
                .collect(Collectors.toList());
            
            List<String> servers = redisTemplate.opsForValue().multiGet(userKeys);
            
            // 3. 내 서버에 연결된 유저만 필터링
            List<Long> myUsers = new ArrayList<>();
            for (int i = 0; i < participants.size(); i++) {
                Long userId = participants.get(i).getUser().getId();
                String targetServer = servers.get(i);
                
                // 발신자는 이미 동기로 받았으므로 제외
                if (userId.equals(event.getSenderId())) {
                    log.debug("[Kafka Consumer] Skipping sender: userId={}", userId);
                    continue;
                }
                
                // 내 서버에 연결된 유저만
                if (myServer.equals(targetServer)) {
                    myUsers.add(userId);
                }
            }
            
            log.info("[Kafka Consumer] Delivering to {} users on this server ({})", 
                    myUsers.size(), myServer);
            
            // 4. WebSocket으로 전달
            Map<String, Object> messageData = Map.of(
                "messageId", event.getMessageId(),
                "senderId", event.getSenderId(),
                "content", event.getContent(),
                "chatRoomId", event.getChatRoomId(),
                "sentAt", event.getSentAt().toString()
            );
            
            int successCount = 0;
            int failCount = 0;
            
            for (Long userId : myUsers) {
                try {
                    sessionManager.sendToUser(userId, messageData);
                    successCount++;
                    log.debug("[Kafka Consumer] Delivered to user: userId={}", userId);
                } catch (Exception e) {
                    failCount++;
                    log.error("[Kafka Consumer] Failed to deliver to user: userId={}", userId, e);
                }
            }
            
            log.info("[Kafka Consumer] Delivery complete: success={}, failed={}, total={}", 
                    successCount, failCount, myUsers.size());
            
            // 5. 처리 완료 후 수동 커밋
            ack.acknowledge();
            log.debug("[Kafka Consumer] Message acknowledged: messageId={}", event.getMessageId());
            
        } catch (Exception e) {
            log.error("[Kafka Consumer] Failed to process message: messageId={}", 
                    event.getMessageId(), e);
            // 재처리를 위해 커밋하지 않음 (ack.acknowledge() 호출 안 함)
        }
    }
}






