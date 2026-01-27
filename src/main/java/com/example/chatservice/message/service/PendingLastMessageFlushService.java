package com.example.chatservice.message.service;

import com.example.chatservice.chat.repository.UserChatRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 핫 구간에서 스킵된 lastMessageId를 지연 플러시로 밀어 넣는 전용 컴포넌트.
 * KEYS 명령어 대신 Hash를 사용하여 비블로킹 방식으로 처리한다.
 */
@Slf4j
@Service
public class PendingLastMessageFlushService {

    private static final String FLUSH_QUEUE = "chat:lastMsg:flushQueue";
    private static final String PENDING_HASH_KEY = "chat:pendingLastMessages";
    private static final Duration PENDING_TTL = Duration.ofMinutes(10);

    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserChatRepository userChatRepository;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService consumerExecutor;

    public PendingLastMessageFlushService(
            RedissonClient redissonClient,
            RedisTemplate<String, Object> redisTemplate,
            UserChatRepository userChatRepository,
            TransactionTemplate transactionTemplate,
            @Qualifier("lastMessageFlushExecutor") ExecutorService consumerExecutor) {
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
        this.userChatRepository = userChatRepository;
        this.transactionTemplate = transactionTemplate;
        this.consumerExecutor = consumerExecutor;
    }

    private RBlockingQueue<Long> flushQueue;
    private RDelayedQueue<Long> delayedQueue;

    @PostConstruct
    void startConsumer() {
        this.flushQueue = redissonClient.getBlockingQueue(FLUSH_QUEUE);
        this.delayedQueue = redissonClient.getDelayedQueue(flushQueue);

        // 서버 재시작 시 미처리된 pending 메시지 복구
        recoverPendingFlushes();

        consumerExecutor.submit(this::consume);
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down PendingLastMessageFlushService, flushing all pending messages...");

        // 서버 종료 시 모든 pending 메시지 즉시 flush
        flushAllPendingMessages();

        // Executor 종료
        consumerExecutor.shutdownNow();
        try {
            // 남은 작업 완료 대기 (최대 5초)
            if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for executor termination");
        }

        if (delayedQueue != null) {
            delayedQueue.destroy();
        }

        log.info("PendingLastMessageFlushService shutdown completed");
    }

    /**
     * 서버 종료 시 Redis에 남아있는 모든 pending 메시지를 즉시 flush한다.
     * KEYS 명령어 대신 Hash entries를 사용하여 비블로킹 방식으로 처리한다.
     */
    private void flushAllPendingMessages() {
        try {
            // Hash에서 모든 pending 메시지 조회 (비블로킹)
            Map<Object, Object> allPending = redisTemplate.opsForHash().entries(PENDING_HASH_KEY);
            if (allPending.isEmpty()) {
                return;
            }

            for (Map.Entry<Object, Object> entry : allPending.entrySet()) {
                try {
                    Long chatRoomId = Long.parseLong(entry.getKey().toString());
                    flush(chatRoomId); // 동기적으로 실행되므로 완료될 때까지 기다림
                } catch (Exception e) {
                    log.error("Error flushing pending chatRoomId={} on shutdown", entry.getKey(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during shutdown flush", e);
        }
    }

    /**
     * 디바운스 구간에 스킵된 lastMessageId를 캐싱하고 지연 플러시를 예약한다.
     * 이미 스케줄링된 경우 새로운 스케줄링을 하지 않는다 (중복 방지).
     */
    public void scheduleFlush(Long chatRoomId, Long messageId, Duration delay) {
        // 최대 messageId를 Redis Hash에 캐싱 (이미 스케줄링되어 있어도 최신 값으로 업데이트)
        cachePendingMax(chatRoomId, messageId);

        // 이미 스케줄링되어 있으면 추가하지 않음
        String scheduledKey = scheduledKey(chatRoomId);
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(scheduledKey, "1", delay);

        if (Boolean.TRUE.equals(wasAbsent)) {
            // 처음 스케줄링하는 경우에만 큐에 추가
            delayedQueue.offer(chatRoomId, delay.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Scheduled pending lastMessage flush: chatRoomId={}, messageId={}, delayMs={}",
                    chatRoomId, messageId, delay.toMillis());
        } else {
            // 이미 스케줄링되어 있음 - messageId만 업데이트하고 큐에는 추가하지 않음
            log.debug("Flush already scheduled for chatRoomId={}, updating messageId to {}",
                    chatRoomId, messageId);
        }
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

    /**
     * 핫모드가 끝났을 때 pending이 있으면 즉시 flush하고 정리한다.
     * 핫모드에서 쿨모드로 전환될 때 메모리 누수를 방지하기 위해 사용된다.
     */
    public void flushIfPending(Long chatRoomId) {
        // Hash 필드 존재 확인
        if (!redisTemplate.opsForHash().hasKey(PENDING_HASH_KEY, chatRoomId.toString())) {
            // pending이 없으면 아무것도 하지 않음
            return;
        }

        // pending이 있으면 즉시 flush (핫모드 종료 시 정리)
        log.debug("Flushing pending message on hot mode exit: chatRoomId={}", chatRoomId);
        flush(chatRoomId);
    }

    private void flush(Long chatRoomId) {
        // Hash에서 개별 필드 조회
        Object pendingRaw = redisTemplate.opsForHash().get(PENDING_HASH_KEY, chatRoomId.toString());
        if (!(pendingRaw instanceof String pendingIdStr)) {
            log.debug("No pending lastMessageId found for chatRoomId={}", chatRoomId);
            // 스케줄링 플래그도 정리
            redisTemplate.delete(scheduledKey(chatRoomId));
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
            // flush 완료 후 관련 Redis 키 모두 정리
            redisTemplate.opsForHash().delete(PENDING_HASH_KEY, chatRoomId.toString());
            redisTemplate.delete(scheduledKey(chatRoomId));
        }
    }

    private void cachePendingMax(Long chatRoomId, Long messageId) {
        // Hash에서 현재 값 조회
        Object currentRaw = redisTemplate.opsForHash().get(PENDING_HASH_KEY, chatRoomId.toString());

        try {
            if (currentRaw == null || Long.parseLong(currentRaw.toString()) < messageId) {
                // Hash 필드에 저장 (최대값만 유지)
                redisTemplate.opsForHash().put(PENDING_HASH_KEY, chatRoomId.toString(), String.valueOf(messageId));
                // Hash 전체에 TTL 설정
                redisTemplate.expire(PENDING_HASH_KEY, PENDING_TTL);
                log.debug("Cached pending lastMessageId: chatRoomId={}, messageId={}", chatRoomId, messageId);
            }
        } catch (NumberFormatException e) {
            // 파싱 오류 시 새로 저장
            redisTemplate.opsForHash().put(PENDING_HASH_KEY, chatRoomId.toString(), String.valueOf(messageId));
            redisTemplate.expire(PENDING_HASH_KEY, PENDING_TTL);
            log.debug("Cached pending lastMessageId (reset due to parse error): chatRoomId={}, messageId={}", chatRoomId, messageId);
        }
    }

    private String scheduledKey(Long chatRoomId) {
        return "chat:%d:flushScheduled".formatted(chatRoomId);
    }

    /**
     * 서버 재시작 시 Redis에 남아있는 pending 메시지들을 복구한다.
     * 서버가 크래시된 경우 scheduledKey는 TTL로 만료되었지만 pendingKey는 남아있을 수 있다.
     * KEYS 명령어 대신 Hash entries를 사용하여 비블로킹 방식으로 처리한다.
     */
    private void recoverPendingFlushes() {
        try {
            // Hash에서 모든 pending 메시지 조회 (비블로킹)
            Map<Object, Object> allPending = redisTemplate.opsForHash().entries(PENDING_HASH_KEY);

            if (allPending == null || allPending.isEmpty()) {
                log.debug("No pending messages to recover");
                return;
            }

            int recoveredCount = 0;

            for (Map.Entry<Object, Object> entry : allPending.entrySet()) {
                try {
                    Long chatRoomId = Long.parseLong(entry.getKey().toString());
                    String scheduledKey = scheduledKey(chatRoomId);

                    // scheduledKey가 없거나 만료된 경우 → 서버 크래시 후 복구 상황
                    // 큐에 의존하지 않고 즉시 flush하여 데이터 손실 방지
                    if (!redisTemplate.hasKey(scheduledKey)) {
                        Object pendingRaw = entry.getValue();
                        if (pendingRaw instanceof String pendingIdStr) {
                            try {
                                Long messageId = Long.parseLong(pendingIdStr);
                                // 즉시 flush (서버 크래시로 지연된 것들이므로 즉시 처리)
                                log.info("Recovering pending flush from server crash: chatRoomId={}, messageId={}",
                                        chatRoomId, messageId);
                                flush(chatRoomId);
                                recoveredCount++;
                            } catch (NumberFormatException e) {
                                log.warn("Invalid pending messageId for chatRoomId={}, deleting", chatRoomId);
                                redisTemplate.opsForHash().delete(PENDING_HASH_KEY, chatRoomId.toString());
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid chatRoomId in pending entry: {}", entry.getKey());
                } catch (Exception e) {
                    log.error("Error recovering pending chatRoomId={}", entry.getKey(), e);
                }
            }

            if (recoveredCount > 0) {
                log.info("Recovered {} pending flush(es) on server startup", recoveredCount);
            }
        } catch (Exception e) {
            log.error("Error during pending flush recovery", e);
        }
    }
}