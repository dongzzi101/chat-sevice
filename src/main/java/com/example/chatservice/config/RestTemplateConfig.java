package com.example.chatservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 간단한 방법: 기본 RestTemplate에 타임아웃만 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 연결 타임아웃 5초
        factory.setReadTimeout(10000); // 읽기 타임아웃 10초
        // factory.setBufferRequestBody(true); // 요청 본문을 버퍼링하여 재시도 가능하게 함 deprecate
        
        return new RestTemplate(factory);
    }

}
