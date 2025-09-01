package com.example.chatservice.message.entity;

import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    private Chat chatRoom;

    @Builder
    public Message(String message, User sender, User receiver) {
        this.message = message;
        this.sender = sender;
        this.receiver = receiver;
    }

    // TODO
    /*
    Message.receiver 제거
    1:1 개인톡도 Chat 테이블에 채팅방 생성
    모든 메시지는 sender + chatRoom으로 저장 및 조회
     */
}
