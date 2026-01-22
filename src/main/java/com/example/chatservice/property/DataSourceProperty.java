package com.example.chatservice.property;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ToString
@Setter
@Getter
@ConfigurationProperties(prefix = "app.datasource")
public class DataSourceProperty {
    private MainDataSource main;
    private MessageDataSource message;
    
    @ToString
    @Setter
    @Getter
    public static class MainDataSource {
        private String username;
        private String password;
        private String url;
        private String driverClassName;
        private int maxPoolSize = 20;
        private int minIdle = 5;
    }
    
    @ToString
    @Setter
    @Getter
    public static class MessageDataSource {
        private String username;
        private String password;
        private String driverClassName;
        private int maxPoolSize = 20;
        private int minIdle = 5;
        private List<ShardInfo> shards;
        
        @ToString
        @Setter
        @Getter
        public static class ShardInfo {
            private String key;
            private String url;
        }
    }
}
