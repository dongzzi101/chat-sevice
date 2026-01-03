package com.example.chatservice.config.dataSource;

import com.example.chatservice.sharding.MessageShardKeySelector;
import com.example.chatservice.sharding.ShardingAspect;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.Map;

public class ShardingDataSource extends AbstractRoutingDataSource {

    private final MessageShardKeySelector messageShardKeySelector;

    public ShardingDataSource(Map<Object, Object> dataSourceMap, MessageShardKeySelector messageShardKeySelector) {
        this.messageShardKeySelector = messageShardKeySelector;
        super.setTargetDataSources(dataSourceMap);
        this.afterPropertiesSet();
    }


    @Override
    protected Object determineCurrentLookupKey() {
        Object key;
        Long currentThreadChatId = ShardingAspect.getCurrentThreadChatId();
        key = messageShardKeySelector.getShardKey(currentThreadChatId);
        System.out.println("SHARD_KEY=" + key);
        return key;
    }
}
