package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserChatRepository extends JpaRepository<UserChat, Long> {

    void deleteByUserAndChat(User user, ChatRoom chatRoom);

}
