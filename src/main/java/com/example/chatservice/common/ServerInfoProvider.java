package com.example.chatservice.common;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Slf4j
@Component
public class ServerInfoProvider {

    @Value("${server.port}")
    private int port;

    @Getter
    private String serverAddress;

    @PostConstruct
    public void init() {
        try {
            this.serverAddress = "localhost:" + port;
            String ip = InetAddress.getLocalHost().getHostAddress();
            
            log.info("========================================");
            log.info("Server started at: {} (IP: {})", serverAddress, ip);
            log.info("========================================");

        } catch (Exception e) {
            this.serverAddress = "localhost:" + port;
            log.warn("Failed to get IP address, using localhost: {}", serverAddress);
        }
    }

}
