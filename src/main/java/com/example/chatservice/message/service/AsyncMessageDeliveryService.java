package com.example.chatservice.message.service;

import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.common.ServerInfoProvider;
import com.example.chatservice.common.SessionManager;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class AsyncMessageDeliveryService {

    private final MessageRepository messageRepository;
    private final UserChatRepository userChatRepository;
    private final MessageDeliveryService messageDeliveryService; // 재사용

    @Async("messageExecutor")
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
//        List<UserChat> userChats = userChatRepository.findByChatRoomIdAndLeavedAtIsNull(chatRoomId);

        log.info("[Async] Delivering message to {} participants in chatRoom {}",
                userChats.size(), chatRoomId);

        for (UserChat userChat : userChats) {
            Long participantId = userChat.getUser().getId();

            // 발신자는 이미 받았으므로 제외
            if (participantId.equals(senderId)) {
                log.info("[Async] Skipping sender {} (already received)", participantId);
                continue;
            }

            log.info("[Async] Attempting to deliver message to participant {}", participantId);
            messageDeliveryService.deliverMessage(participantId, message);
        }
    }


}
