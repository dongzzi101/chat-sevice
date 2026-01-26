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
    private int windowSeconds;
    private int modeTtlSeconds;
    private int debounceSeconds;
    private long enterThreshold;
    private long exitThreshold;
}
