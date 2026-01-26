package com.example.chatservice.config;

import com.example.chatservice.property.MessageForwardRetryProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableConfigurationProperties(MessageForwardRetryProperty.class)
public class MessageRetryExecutorConfig {

    @Bean(name = "retryScheduler")
    public ScheduledExecutorService retryScheduler() {
        return Executors.newScheduledThreadPool(2);
    }

}
