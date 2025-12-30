package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChatRepository extends JpaRepository<UserChat, Long> {
/**

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

*/

    @Query("SELECT uc FROM UserChat uc JOIN FETCH uc.user WHERE uc.chatRoom.id = :chatRoomId AND uc.leavedAt IS NULL")
    List<UserChat> findActiveByChatRoomWithUser(@Param("chatRoomId") Long chatRoomId);

    @Query("SELECT uc FROM UserChat uc WHERE uc.user.id = :userId ORDER BY uc.lastMessageId DESC")
    List<UserChat> findByUserIdOrderByLastMessageIdDesc(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserChat uc
            SET uc.lastMessageId = :messageId
            WHERE uc.chatRoom.id = :chatRoomId
              AND uc.leavedAt IS NULL
              AND (uc.lastMessageId IS NULL OR uc.lastMessageId < :messageId)
            """)
    int updateLastMessageIdForChat(@Param("chatRoomId") Long chatRoomId,
                                   @Param("messageId") Long messageId);

    boolean existsByUserAndChatRoomAndLeavedAtIsNull(User user, ChatRoom chatRoom);

    Optional<UserChat> findByUserAndChatRoomAndLeavedAtIsNull(User user, ChatRoom chatRoom);
}
