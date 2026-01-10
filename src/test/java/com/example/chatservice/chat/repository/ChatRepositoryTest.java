package com.example.chatservice.chat.repository;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ChatRepositoryTest {

    @Autowired
    private ChatRepository chatRepository;

    @Test
    @DisplayName("chatKey로 채팅방을 찾을 수 있다.")
    void findChatRoomByChatKey() {
        // given
        String targetChatKey = "test";

        ChatRoom chatRoom = ChatRoom.builder()
                .chatKey(targetChatKey)
                .type(ChatType.IM)
                .build();

        chatRepository.save(chatRoom);

        // when
        ChatRoom findChatRoom = chatRepository.findChatRoomByChatKey(targetChatKey).orElseThrow();

        // then
        assertThat(findChatRoom.getChatKey()).isEqualTo(targetChatKey);
    }

    @Test
    @DisplayName("chatKey로 채팅방을 찾을 수 없으면 null을 반환한다.")
    void returnNullIfChatKeyIsNull() {
        // given
        String chatKey = "test";
        String wrongChatKey = "test2";

        ChatRoom chatRoom = ChatRoom.builder()
                .chatKey(chatKey)
                .type(ChatType.IM)
                .build();

        chatRepository.save(chatRoom);

        // when
        Optional<ChatRoom> chatRoomByChatKey = chatRepository.findChatRoomByChatKey(wrongChatKey);

        // then
        assertThat(chatRoomByChatKey).isNotPresent();
    }


}