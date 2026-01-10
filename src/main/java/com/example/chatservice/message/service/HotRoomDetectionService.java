package com.example.chatservice.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotRoomDetectionService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final Duration HOT_WINDOW = Duration.ofSeconds(5);
    private static final Duration HOT_MODE_TTL = Duration.ofSeconds(30);
    private static final Duration HOT_DEBOUNCE = Duration.ofSeconds(3);
    private static final long HOT_ENTER_THRESHOLD = 5L;
    private static final long HOT_EXIT_THRESHOLD = 2L;

    public boolean isHotRoom(Long chatRoomId) {
        String countKey = msgCountKey(chatRoomId);
        Long count = redisTemplate.opsForValue().increment(countKey);
        redisTemplate.expire(countKey, HOT_WINDOW);

        String modeKey = modeKey(chatRoomId);
        String mode = (String) redisTemplate.opsForValue().get(modeKey);

        if (count != null && count >= HOT_ENTER_THRESHOLD) {
            if (!"hot".equals(mode)) {
                redisTemplate.opsForValue().set(modeKey, "hot", HOT_MODE_TTL);
            }
            return true;
        }

        if ("hot".equals(mode) && count != null && count <= HOT_EXIT_THRESHOLD) {
            redisTemplate.opsForValue().set(modeKey, "cool", HOT_MODE_TTL);
            return false;
        }

        return "hot".equals(mode);
    }

    public boolean shouldSkipHotUpdate(Long chatRoomId) {
        String lastKey = lastAppliedKey(chatRoomId);
        String lastTs = (String) redisTemplate.opsForValue().get(lastKey);
        long now = System.currentTimeMillis();

        if (lastTs != null) {
            try {
                long last = Long.parseLong(lastTs);
                if (now - last < HOT_DEBOUNCE.toMillis()) {
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid last applied timestamp for chatRoomId={}", chatRoomId);
            }
        }

        redisTemplate.opsForValue().set(lastKey, String.valueOf(now), HOT_MODE_TTL);
        return false;
    }

    public Duration getDebounceDuration() {
        return HOT_DEBOUNCE;
    }

    private String msgCountKey(Long chatRoomId) {
        return "chat:%d:msgCount".formatted(chatRoomId);
    }

    private String modeKey(Long chatRoomId) {
        return "chat:%d:mode".formatted(chatRoomId);
    }

    private String lastAppliedKey(Long chatRoomId) {
        return "chat:%d:lastApplied".formatted(chatRoomId);
    }
}

