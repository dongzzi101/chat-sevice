package com.example.chatservice.message.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PendingLastMessageFlushServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOps;

    @InjectMocks
    PendingLastMessageFlushService pendingLastMessageFlushService;

    @Test
    @DisplayName("cachePendingMax는 더 큰 lastMessageId일 때만 set/expire 한다")
    void cachePendingMaxTest() {
        Long chatRoomId = 1L;
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("chat:1:pendingLastMessage")).thenReturn("5");

        ReflectionTestUtils.invokeMethod(pendingLastMessageFlushService, "cachePendingMax", chatRoomId, 4L);
        verify(valueOps, never()).set(any(), any());

        ReflectionTestUtils.invokeMethod(pendingLastMessageFlushService, "cachePendingMax", chatRoomId, 6L);
        verify(valueOps).set("chat:1:pendingLastMessage", "6");
        verify(redisTemplate).expire("chat:1:pendingLastMessage", Duration.ofMinutes(10));
    }


}