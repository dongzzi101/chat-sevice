package com.example.chatservice.message.service;

import com.example.chatservice.property.HotRoomProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(HotRoomProperty.class)
public class HotRoomDetectionService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HotRoomProperty hotRoomProperty;

    public boolean isHotRoom(Long chatRoomId) {
        Duration hotWindow = Duration.ofSeconds(hotRoomProperty.getWindowSeconds());
        Duration hotModeTtl = Duration.ofSeconds(hotRoomProperty.getModeTtlSeconds());

        String countKey = msgCountKey(chatRoomId);

        // Pipeline으로 increment + expire를 한 번에 실행
        List<Object> results = redisTemplate.executePipelined(
                (RedisCallback<Object>) connection -> {
                    RedisConnection redisConn = (RedisConnection) connection;
                    byte[] keyBytes = countKey.getBytes();

                    redisConn.stringCommands().incr(keyBytes);
                    redisConn.keyCommands().expire(keyBytes, hotWindow.getSeconds());
                    return null;
                }
        );

        // Pipeline 결과에서 count 추출: results[0] = incr 결과, results[1] = expire 결과(사ㅓ용 x)
        Long count = (Long) results.getFirst();

        String modeKey = modeKey(chatRoomId);
        String mode = (String) redisTemplate.opsForValue().get(modeKey);

        if (count != null && count >= hotRoomProperty.getEnterThreshold()) {
            if (!"hot".equals(mode)) {
                redisTemplate.opsForValue().set(modeKey, "hot", hotModeTtl);
            }
            return true;
        }

        if ("hot".equals(mode) && count != null && count <= hotRoomProperty.getExitThreshold()) {
            redisTemplate.opsForValue().set(modeKey, "cool", hotModeTtl);
            return false;
        }

        return "hot".equals(mode);
    }

    public boolean shouldSkipHotUpdate(Long chatRoomId) {
        Duration hotDebounce = Duration.ofSeconds(hotRoomProperty.getDebounceSeconds());
        Duration hotModeTtl = Duration.ofSeconds(hotRoomProperty.getModeTtlSeconds());

        String lastKey = lastAppliedKey(chatRoomId);
        String lastTs = (String) redisTemplate.opsForValue().get(lastKey);
        long now = System.currentTimeMillis();

        if (lastTs != null) {
            try {
                long last = Long.parseLong(lastTs);
                if (now - last < hotDebounce.toMillis()) {
                    return true;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid last applied timestamp for chatRoomId={}", chatRoomId);
            }
        }

        redisTemplate.opsForValue().set(lastKey, String.valueOf(now), hotModeTtl);
        return false;
    }

    public Duration getDebounceDuration() {
        return Duration.ofSeconds(hotRoomProperty.getDebounceSeconds());
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

