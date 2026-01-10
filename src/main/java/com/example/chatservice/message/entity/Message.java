package com.example.chatservice.message.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder
    public Message(Long id, String message, Long senderId, Long chatRoomId) {
        this.id = id;
        this.message = message;
        this.senderId = senderId;
        this.chatRoomId = chatRoomId;
        this.createdAt = LocalDateTime.now();
    }
}
