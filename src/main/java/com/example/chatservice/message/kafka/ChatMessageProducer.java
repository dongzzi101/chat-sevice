package com.example.chatservice.message.kafka;

import com.example.chatservice.message.event.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka Producer
 * 채팅 메시지를 Kafka 토픽에 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatMessageProducer {

    private static final String TOPIC = "chat-messages";
    
    private final KafkaTemplate<String, ChatMessageEvent> kafkaTemplate;
    
    /**
     * 채팅 메시지를 Kafka 토픽에 발행
     */
    public void sendMessage(ChatMessageEvent event) {
        try {
            // chatRoomId를 key로 사용 -> 같은 채팅방의 메시지는 같은 파티션으로 (순서 보장)
            String key = event.getChatRoomId().toString();
            
            kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to send message: messageId={}, chatRoomId={}", 
                                event.getMessageId(), event.getChatRoomId(), ex);
                    } else {
                        log.info("[Kafka] Message sent successfully: messageId={}, chatRoomId={}, partition={}", 
                                event.getMessageId(), 
                                event.getChatRoomId(),
                                result.getRecordMetadata().partition());
                    }
                });
            
        } catch (Exception e) {
            log.error("[Kafka] Exception while sending message: messageId={}", 
                    event.getMessageId(), e);
        }
    }
}






