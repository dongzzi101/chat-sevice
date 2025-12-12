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
    private Long id;

    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChatRoom chatRoom;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Message(Long id, String message, User sender, ChatRoom chatRoom) {
        this.id = id;
        this.message = message;
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.createdAt = LocalDateTime.now();
    }
}
