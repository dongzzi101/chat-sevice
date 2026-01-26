package com.example.chatservice.chat.entity;

import com.example.chatservice.common.BaseEntity;
import com.example.chatservice.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "read_status")
public class ReadStatus extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;


    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Builder
    public ReadStatus(User user, ChatRoom chatRoom, Long lastReadMessageId) {
        this.user = user;
        this.chatRoom = chatRoom;
        this.lastReadMessageId = lastReadMessageId;
    }

    public void updateReadMessage(Long messageId) {
        this.lastReadMessageId = messageId;
    }
}
