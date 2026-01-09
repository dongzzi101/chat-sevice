package com.example.chatservice.message.controller;

import com.example.chatservice.chat.sercivce.ChatService;
import com.example.chatservice.config.WebMvcTestConfig;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.service.MessageService;
import com.example.chatservice.user.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@ContextConfiguration(classes = {MessageController.class, WebMvcTestConfig.class})
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private ChatService chatService;

    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        testUserPrincipal = new UserPrincipal(1L, "testUser");
    }

    @Test
    @DisplayName("메시지 조회를 할 수 있다.")
    void getMessages() throws Exception {
        // given
        Long chatRoomId = 1L;

        MessageResponse messageResponse = MessageResponse.builder()
                .message("hello")
                .senderId(1L)
                .build();

        List<MessageResponse> messageResponses = List.of(messageResponse);

        given(messageService.getMessages(
                any(Long.class), any(Long.class), any(Long.class), any(Integer.class), any(Integer.class)
        ))
                .willReturn(messageResponses);


        // when & then
        mockMvc.perform(get("/api/v1/messages/{chatRoomId}", chatRoomId)
                        .requestAttr("userPrincipal", testUserPrincipal))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"before", "after", "lastReadMessageId"})
    @DisplayName("메시지 조회 시 lastReadMessageId, before, after 파라미터는 숫자로 들어와야 한다.")
    void getMessagesParametersWithNumber(String paramName) throws Exception {
        // given
        Long chatRoomId = 1L;

        MessageResponse messageResponse = MessageResponse.builder()
                .message("hello")
                .senderId(1L)
                .build();

        List<MessageResponse> messageResponses = List.of(messageResponse);

        given(messageService.getMessages(
                any(Long.class), any(Long.class), any(Long.class), any(Integer.class), any(Integer.class)
        ))
                .willReturn(messageResponses);


        // when & then
        mockMvc.perform(get("/api/v1/messages/{chatRoomId}", chatRoomId)
                        .requestAttr("userPrincipal", testUserPrincipal)
                        .queryParam(paramName, "abc"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("메시지 읽음처리를 생성한다.")
    void readMessage() throws Exception {
        // given
        Long chatRoomId = 1L;
        Long messageId = 1L;

        mockMvc.perform(post("/api/v1/messages/{chatRoomId}/read", chatRoomId)
                        .requestAttr("userPrincipal", testUserPrincipal)
                        .queryParam("messageId", messageId.toString()))
                .andDo(print())
                .andExpect(status().isOk());

        verify(messageService)
                .markMessagesAsRead(testUserPrincipal.getId(), chatRoomId, messageId);
    }


}