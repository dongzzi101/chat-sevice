package com.example.chatservice.sharding;

public class MessageShardKeySelector {

    public Integer getShardKey(Long chatId) {
        if (chatId == null) {
            return 0;
        }
        return Math.toIntExact(chatId % 2);
    }
}
