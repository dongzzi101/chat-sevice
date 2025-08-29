package com.example.chatservice.user.entity;

import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.entity.UserChat;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @OneToMany(mappedBy = "user")
    private List<UserChat> userChats;

    @Builder
    public User(String username) {
        this.username = username;
    }
}
