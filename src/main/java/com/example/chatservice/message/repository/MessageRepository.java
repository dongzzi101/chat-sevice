package com.example.chatservice.message.repository;

import com.example.chatservice.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);


    Optional<Message> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);
}
