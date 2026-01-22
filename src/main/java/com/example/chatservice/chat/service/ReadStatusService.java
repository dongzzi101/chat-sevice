package com.example.chatservice.chat.service;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ReadStatus;
import com.example.chatservice.chat.repository.ReadStatusRepository;
import com.example.chatservice.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 읽음 상태 관리 서비스
 * 사용자가 메시지를 읽었는지 추적하는 독립적인 도메인 로직을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class ReadStatusService {
    
    private final ReadStatusRepository readStatusRepository;

    @Transactional
    public ReadStatus getOrCreateReadStatus(User user, ChatRoom chatRoom) {
        ReadStatus readStatus = readStatusRepository.findByUserAndChatRoom(user, chatRoom);
        if (readStatus == null) {
            readStatus = ReadStatus.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .lastReadMessageId(null)
                    .build();
            readStatusRepository.save(readStatus);
        }
        return readStatus;
    }
}
