package com.example.chatservice.chat.entity;


import com.example.chatservice.user.entity.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO 3 : 원래 단체 채팅방 만들 때 이름 정해주지 않나 해서 필드로 받았었는데 이거는 다른곳에서 관리 해야하지않나?
//    private String name;

    @OneToMany(mappedBy = "chat")
    private List<UserChat> userChats;

//    @Builder
//    public Chat(String name) {
//        this.name = name;
//    }
}
