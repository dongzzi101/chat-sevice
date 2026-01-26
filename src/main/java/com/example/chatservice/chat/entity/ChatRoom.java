package com.example.chatservice.chat.entity;


import com.example.chatservice.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chatroom")
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChatType type; // direct, group

    @OneToMany(mappedBy = "chatRoom")
    private List<UserChat> userChats = new ArrayList<>();

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
