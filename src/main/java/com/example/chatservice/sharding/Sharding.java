package com.example.chatservice.sharding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Sharding {

    ShardingTarget target();

    /**
     * SpEL expression to resolve shard key (e.g. "#chatRoomId").
     */
    String key();

}
