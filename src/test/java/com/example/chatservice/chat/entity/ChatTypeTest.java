package com.example.chatservice.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTypeTest {

    @Test
    @DisplayName("각 메시지방 챗타입을 확인한다.")
    void chatType() {
        // given
        String directText = ChatType.DIRECT.getText();
        String groupText = ChatType.GROUP.getText();
        String imText = ChatType.IM.getText();

        // when & then
        assertThat(directText).isEqualTo("일대일방");
        assertThat(groupText).isEqualTo("그룹방");
        assertThat(imText).isEqualTo("자신한테 보내는 메시지방");
    }


}