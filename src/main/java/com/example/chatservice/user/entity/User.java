package com.example.chatservice.user.entity;

import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Builder
    public User(String username) {
        this.username = username;
    }
}
