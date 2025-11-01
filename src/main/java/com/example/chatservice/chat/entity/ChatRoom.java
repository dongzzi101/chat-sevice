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

    // TODO 3 : 원래 단체 채팅방 만들 때 이름 정해주지 않나 해서 필드로 받았었는데 이거는 다른곳에서 관리 해야하지않나?
//    private String name = ""; // null

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
