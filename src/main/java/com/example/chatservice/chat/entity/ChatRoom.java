package com.example.chatservice.chat.entity;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chatroom")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // -> enum 이 더 좋은 선택
    @Enumerated(EnumType.STRING)
    private ChatType type; // direct, group

    @OneToMany(mappedBy = "chatRoom")
    private List<UserChat> userChats;

    private String chatKey;

    @Builder
    public ChatRoom(ChatType type, String chatKey) {
        this.type =  type;
        this.chatKey = chatKey;
    }

    public void updateChatKey(String chatKey) {
        this.chatKey = chatKey;
    }
}
