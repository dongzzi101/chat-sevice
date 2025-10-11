package com.example.chatservice.message.entity;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

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

    // TODO : 굳이 receiver 필요할까?
    // 1. user -> message -> chat Room -> 알아서 뿌려주면 되는 건가?
//    @ManyToOne(fetch = FetchType.LAZY)
//    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Message(String message, User sender, ChatRoom chatRoom) {
        this.message = message;
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.createdAt = LocalDateTime.now();
    }
}
