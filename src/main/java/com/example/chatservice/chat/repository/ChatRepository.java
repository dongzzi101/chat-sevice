package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findChatRoomByChatKey(String chatKey);


}
