package com.example.chatservice.user;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}

//TODO 2 : CurrentUser 는 어는 패키지에 넣어야하지?
// 1. auth 패키지를 만든다.
// 2. user 에서 처리한다.


