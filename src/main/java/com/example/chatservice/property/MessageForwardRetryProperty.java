package com.example.chatservice.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "message.forward.retry")
public class MessageForwardRetryProperty {

    private int maxRetries;
    private long baseDelayMs;

}
