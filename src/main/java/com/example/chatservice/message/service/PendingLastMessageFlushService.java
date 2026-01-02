package com.example.chatservice.message.service;

import com.example.chatservice.chat.repository.UserChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 핫 구간에서 스킵된 lastMessageId를 지연 플러시로 밀어 넣는 전용 컴포넌트.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PendingLastMessageFlushService {

    private static final String FLUSH_QUEUE = "chat:lastMsg:flushQueue";
    private static final Duration PENDING_TTL = Duration.ofMinutes(10);

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserChatRepository userChatRepository;
    private final TransactionTemplate transactionTemplate;

    @Qualifier("lastMessageFlushExecutor")
    private final ExecutorService consumerExecutor;

    private RBlockingQueue<Long> flushQueue;
    private RDelayedQueue<Long> delayedQueue;

    @PostConstruct
    void startConsumer() {
        this.flushQueue = redissonClient.getBlockingQueue(FLUSH_QUEUE);
        this.delayedQueue = redissonClient.getDelayedQueue(flushQueue);

        consumerExecutor.submit(this::consume);
    }

    @PreDestroy
    void shutdown() {
        consumerExecutor.shutdownNow();
        if (delayedQueue != null) {
            delayedQueue.destroy();
        }
    }

    /**
     * 디바운스 구간에 스킵된 lastMessageId를 캐싱하고 지연 플러시를 예약한다.
     */
    public void scheduleFlush(Long chatRoomId, Long messageId, Duration delay) {
        cachePendingMax(chatRoomId, messageId);
        log.debug("Scheduled pending lastMessage flush: chatRoomId={}, messageId={}, delayMs={}",
                chatRoomId, messageId, delay.toMillis());
        delayedQueue.offer(chatRoomId, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void consume() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Long chatRoomId = flushQueue.take(); // 딜레이가 지난 항목만 도착
                log.debug("Flush dequeue received chatRoomId={}", chatRoomId);
                flush(chatRoomId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Unexpected error while flushing lastMessageId", e);
        }
    }

    private void flush(Long chatRoomId) {
        String pendingKey = pendingKey(chatRoomId);
        Object pendingRaw = redisTemplate.opsForValue().get(pendingKey);
        if (!(pendingRaw instanceof String pendingIdStr)) {
            log.debug("No pending lastMessageId found for chatRoomId={}, key={}", chatRoomId, pendingKey);
            return;
        }

        try {
            Long pendingId = Long.parseLong(pendingIdStr);
            transactionTemplate.executeWithoutResult(status ->
                    userChatRepository.updateLastMessageIdForChat(chatRoomId, pendingId)
            );
            log.info("Flushed pending lastMessageId: chatRoomId={}, messageId={}", chatRoomId, pendingId);
        } catch (NumberFormatException e) {
            log.warn("Invalid pending lastMessageId for chatRoomId={}", chatRoomId);
        } finally {
            redisTemplate.delete(pendingKey);
        }
    }

    private void cachePendingMax(Long chatRoomId, Long messageId) {
        String key = pendingKey(chatRoomId);
        Object currentRaw = redisTemplate.opsForValue().get(key);

        try {
            if (currentRaw == null || Long.parseLong(currentRaw.toString()) < messageId) {
                redisTemplate.opsForValue().set(key, String.valueOf(messageId));
                redisTemplate.expire(key, PENDING_TTL);
                log.debug("Cached pending lastMessageId: chatRoomId={}, messageId={}", chatRoomId, messageId);
            }
        } catch (NumberFormatException e) {
            redisTemplate.opsForValue().set(key, String.valueOf(messageId));
            redisTemplate.expire(key, PENDING_TTL);
            log.debug("Cached pending lastMessageId (reset due to parse error): chatRoomId={}, messageId={}", chatRoomId, messageId);
        }
    }

    private String pendingKey(Long chatRoomId) {
        return "chat:%d:pendingLastMessage".formatted(chatRoomId);
    }
}

