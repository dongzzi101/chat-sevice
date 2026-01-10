package com.example.chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "messageExecutor")
    public Executor asyncExecutor() {
        return new ThreadPoolExecutor(
                20, 20,
                1L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(10000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy() //.CallerRunsPolicy() //.AbortPolicy()
        );
    }

    @Bean(name = "lastMessageFlushExecutor")
    public ExecutorService lastMessageFlushExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1,
                0L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    Thread t = new Thread(r, "last-message-flush-consumer");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        executor.allowCoreThreadTimeOut(false);

        return executor;
    }
}
