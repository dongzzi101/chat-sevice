package com.example.chatservice.message.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PendingLastMessageFlushServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    HashOperations<String, Object, Object> hashOps;

    @InjectMocks
    PendingLastMessageFlushService pendingLastMessageFlushService;

    @Test
    @DisplayName("cachePendingMax는 더 큰 lastMessageId일 때만 Hash에 put/expire 한다")
    void cachePendingMaxTest() {
        Long chatRoomId = 1L;
        String hashKey = "chat:pendingLastMessages";
        String field = "1";
        
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(hashOps.get(hashKey, field)).thenReturn("5");

        // 더 작은 messageId는 업데이트하지 않음
        ReflectionTestUtils.invokeMethod(pendingLastMessageFlushService, "cachePendingMax", chatRoomId, 4L);
        verify(hashOps, never()).put(eq(hashKey), eq(field), anyString());

        // 더 큰 messageId는 업데이트함
        ReflectionTestUtils.invokeMethod(pendingLastMessageFlushService, "cachePendingMax", chatRoomId, 6L);
        verify(hashOps).put(hashKey, field, "6");
        verify(redisTemplate).expire(hashKey, Duration.ofMinutes(10));
    }

}