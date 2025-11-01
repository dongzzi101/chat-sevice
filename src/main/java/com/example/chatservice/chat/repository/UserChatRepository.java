package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChatRepository extends JpaRepository<UserChat, Long> {

    void deleteByUserAndChatRoom(User user, ChatRoom chatRoom);

    Optional<UserChat> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    @Query("SELECT uc.chatRoom FROM UserChat uc WHERE uc.user.id IN :userIds AND uc.chatRoom.type = 'DIRECT' GROUP BY uc.chatRoom HAVING COUNT(DISTINCT uc.user.id) = 2")
    Optional<ChatRoom> findDirectChatRoomByUserIds(@Param("userIds") List<Long> userIds);
    // chat entity
    // id
    // name
    // chat_key    10_100_DIRECT (오름차순-언더바-DIRECT)
        // chat_key index...

    // chat_key    10_100_1001010_10100_101010_1010_...........................................GROUP
    //             10_100_1001010_10100_101010_1010_...........................................GROUP
    //             hash(...) = 9sd8f98aw3f0sdakfsd0fsadf-sdf

    // invite
    //             10_50_100.......
    //             update hash(...)
    
    @Query("SELECT uc.chatRoom FROM UserChat uc WHERE uc.user.id IN :userIds AND uc.chatRoom.type = 'GROUP' GROUP BY uc.chatRoom HAVING COUNT(DISTINCT uc.user.id) = :userCount")
    Optional<ChatRoom> findGroupChatRoomByUserIds(@Param("userIds") List<Long> userIds, @Param("userCount") long userCount);

    List<UserChat> findByUserId(Long userId);
}
