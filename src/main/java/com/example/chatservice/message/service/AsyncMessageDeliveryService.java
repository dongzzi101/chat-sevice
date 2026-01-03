package com.example.chatservice.message.service;

import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.common.ServerInfoProvider;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.sharding.Sharding;
import com.example.chatservice.sharding.ShardingTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class AsyncMessageDeliveryService {

    private final MessageRepository messageRepository;
    private final UserChatRepository userChatRepository;
    private final MessageDeliveryService messageDeliveryService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ServerInfoProvider serverInfoProvider;

    @Async("messageExecutor")
    @Sharding(target = ShardingTarget.MESSAGE, key = "#chatRoomId")
    @Transactional(readOnly = true, transactionManager = "messageTransactionManager")
    public void deliverMessageAsync(Long senderId, Long receiverId, Long chatRoomId, Long messageId) {
        log.info("[Async] Delivering message {} from sender {} in chatRoom {}",
                messageId, senderId, chatRoomId);

        try {
            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

            if (receiverId != null) {
                // 1:1 채팅 - 수신자에게만 전송 (발신자는 이미 받음)
                messageDeliveryService.deliverMessage(receiverId, message);
            } else {
                // 그룹 채팅 - 발신자 제외하고 전송
                deliverMessageToChatRoom(senderId, chatRoomId, message);
            }

            log.info("[Async] Message {} delivered successfully", messageId);

        } catch (Exception e) {
            log.error("[Async] Failed to deliver message {}", messageId, e);
        }
    }

    private void deliverMessageToChatRoom(Long senderId, Long chatRoomId, Message message) {
        List<UserChat> userChats = userChatRepository.findActiveByChatRoomWithUser(chatRoomId);

        log.info("[Async] Delivering message to {} participants in chatRoom {}",
                userChats.size(), chatRoomId);

        String currentServer = serverInfoProvider.getServerAddress();
        
        // 서버별로 유저 그룹핑
        // Key: 서버 주소 (예: "localhost:8081")
        // Value: 해당 서버에 연결된 유저 ID 목록
        Map<String, List<Long>> serverToUserIds = new HashMap<>();
        
        int sameServerCount = 0;
        int offlineCount = 0;

        for (UserChat userChat : userChats) {
            Long participantId = userChat.getUser().getId();

            // 발신자는 이미 받았으므로 제외
            if (participantId.equals(senderId)) {
                log.debug("[Async] Skipping sender {} (already received)", participantId);
                continue;
            }

            // Redis에서 유저가 연결된 서버 찾기
            String redisKey = "user:" + participantId;
            String targetServer = (String) redisTemplate.opsForValue().get(redisKey);

            if (targetServer == null) {
                // 오프라인 유저 - 스킵
                log.debug("[Async] User {} is offline, skipping", participantId);
                offlineCount++;
                continue;
            }

            if (targetServer.equals(currentServer)) {
                // 같은 서버 - 바로 WebSocket 전송
                log.debug("[Async] User {} is on the same server, sending directly", participantId);
                messageDeliveryService.deliverMessageLocally(participantId, message);
                sameServerCount++;
            } else {
                // 다른 서버 - 그룹에 추가
                serverToUserIds
                    .computeIfAbsent(targetServer, k -> new ArrayList<>())
                    .add(participantId);
            }
        }

        log.info("[Async] Message delivery summary: same server={}, different servers={}, offline={}",
                sameServerCount, 
                serverToUserIds.values().stream().mapToInt(List::size).sum(),
                offlineCount);

        // 각 서버로 배치 전송
        for (Map.Entry<String, List<Long>> entry : serverToUserIds.entrySet()) {
            String targetServer = entry.getKey();
            List<Long> userIds = entry.getValue();

            log.info("[Async] Sending batch message to {} ({} users)", targetServer, userIds.size());
            messageDeliveryService.deliverMessageBatch(targetServer, userIds, message);
        }

        log.info("[Async] Completed delivering message to chatRoom {}. " +
                        "Sent to {} servers via HTTP batch, {} users on same server, {} offline users",
                chatRoomId, serverToUserIds.size(), sameServerCount, offlineCount);
    }


}
