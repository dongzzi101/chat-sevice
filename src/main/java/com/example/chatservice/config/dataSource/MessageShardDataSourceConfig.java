package com.example.chatservice.config.dataSource;

import com.example.chatservice.component.RoutingDataSource;
import com.example.chatservice.property.DataSourceProperty;
import com.example.chatservice.sharding.MessageShardKeySelector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(DataSourceProperty.class)
public class MessageShardDataSourceConfig {
    
    private final DataSourceProperty dataSourceProperty;
    private final DataSourceBuilder dataSourceBuilder;
    
    @Bean(name = "messageDataSource")
    public DataSource messageDataSource() {
        DataSourceProperty.MessageDataSource config = dataSourceProperty.getMessage();
        Map<Object, Object> dataSourceMap = new HashMap<>();

        for (int i = 0; i < config.getShards().size(); i++) {
            DataSourceProperty.MessageDataSource.ShardInfo shard = config.getShards().get(i);
            DataSource shardDataSource = dataSourceBuilder.buildHikariDataSource(
                shard.getUrl(),
                config.getUsername(),
                config.getPassword(),
                config.getDriverClassName(),
                config.getMaxPoolSize(),
                config.getMinIdle()
            );
            dataSourceMap.put(i, shardDataSource);
            log.info("Created message shard DataSource: key={}, url={}", shard.getKey(), shard.getUrl());
        }
        
        MessageShardKeySelector messageShardKeySelector = new MessageShardKeySelector();
        return new LazyConnectionDataSourceProxy(
            new RoutingDataSource(dataSourceMap, messageShardKeySelector)
        );
    }
}

