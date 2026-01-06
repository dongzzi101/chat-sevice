package com.example.chatservice.chat.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatType {

    DIRECT("일대일방"),
    GROUP("그룹방"),
    IM("자신한테 보내는 메시지방");

    private final String text;



}
