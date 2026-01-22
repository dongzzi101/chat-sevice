package com.example.chatservice.property;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ToString
@Setter
@Getter
@ConfigurationProperties(prefix = "app.hot-room")
public class HotRoomProperty {
    private int windowSeconds = 5;
    private int modeTtlSeconds = 30;
    private int debounceSeconds = 3;
    private long enterThreshold = 5L;
    private long exitThreshold = 2L;
}
