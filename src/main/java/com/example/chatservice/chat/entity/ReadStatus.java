package com.example.chatservice.chat.entity;

import com.example.chatservice.message.entity.Message;
import com.example.chatservice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Table(name = "read_status")
public class ReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;


    @ManyToOne
    @JoinColumn(name = "last_read_message_id")
    private Message lastReadMessage;

    @Builder
    public ReadStatus(User user, ChatRoom chatRoom, Message lastReadMessage) {
        this.user = user;
        this.chatRoom = chatRoom;
        this.lastReadMessage = lastReadMessage;
    }

    public void updateReadMessage(Message message) {
        this.lastReadMessage = message;
    }
}
