package com.example.chatservice.message.repository;

import com.example.chatservice.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 특정 메시지 이후의 안읽은 메시지 개수 계산
    long countByChatRoomIdAndIdGreaterThan(Long chatRoomId, Long lastReadMessageId);

    // 채팅방의 전체 메시지 개수
    long countByChatRoomId(Long chatRoomId);

    Optional<Message> findTopByChatRoomIdOrderByIdDesc(Long id);

    List<Message> findByChatRoomIdAndIdLessThanOrderByIdDesc(
            Long chatRoomId, Long messageId, Pageable pageable);

    List<Message> findByChatRoomIdAndIdGreaterThanOrderByIdAsc(
            Long chatRoomId, Long messageId, Pageable pageable);

    Optional<Message> findTopByChatRoomIdOrderByIdAsc(Long chatRoomId);

    // 여러건 업데이트 쿼리
    // jpa 배치 업데이트
    // @Modifying
    // @Query("DELETE m FROM Memssage m... where ...")
}
