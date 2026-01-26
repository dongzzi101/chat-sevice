package com.example.chatservice.chat.entity;

import com.example.chatservice.common.BaseEntity;
import com.example.chatservice.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "user_chat")
public class UserChat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "chat_id")
    private ChatRoom chatRoom;

    private LocalDateTime leavedAt;

    // private lastSeenMessageId // or lastSeenMessageTime

    // 유저별로 채팅방에 마지막 메시지가 다른가? -> 똑같을 듯?
    // TODO:ordering 별도의 키 (쿠팡 상품도 같이 생각해보자)
    // private Long lastMessageId; // snowflake
    // private Long lastMessageTime;

    private Long lastMessageId;

    // private Long favoriteTime; // pinnedAt;

    @Builder
    public UserChat(User user, ChatRoom chatRoom) {
        this.user = user;
        this.chatRoom = chatRoom;
    }

    public void leaveChatRoom() {
        this.leavedAt = LocalDateTime.now();
    }

}
