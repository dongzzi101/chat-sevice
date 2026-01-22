package com.example.chatservice.component;

import com.example.chatservice.sharding.MessageShardKeySelector;
import com.example.chatservice.sharding.ShardingAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Map;

@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    private final MessageShardKeySelector messageShardKeySelector;
    
    public RoutingDataSource(Map<Object, Object> dataSourceMap, MessageShardKeySelector messageShardKeySelector) {
        this.messageShardKeySelector = messageShardKeySelector;
        super.setTargetDataSources(dataSourceMap);
        this.afterPropertiesSet();
    }
    
    @Override
    protected Object determineCurrentLookupKey() {
        Long currentThreadChatId = ShardingAspect.getCurrentThreadChatId();
        Object key = messageShardKeySelector.getShardKey(currentThreadChatId);
        log.debug("Shard key determined: chatRoomId={}, shardKey={}", currentThreadChatId, key);
        return key;
    }
}
