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
            // localhost를 사용하여 일관성 유지
            // 실제 운영 환경에서는 환경 변수나 설정 파일에서 가져오는 것이 좋음
            this.serverAddress = "localhost:" + port;
            
            // IP 주소도 로깅용으로 가져오기
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
