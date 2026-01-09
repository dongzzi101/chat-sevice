package com.example.chatservice.chat.controller;

import com.example.chatservice.chat.controller.reponse.ChatResponse;
import com.example.chatservice.chat.controller.reponse.ChatRoomResponse;
import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.entity.ChatType;
import com.example.chatservice.chat.sercivce.ChatService;
import com.example.chatservice.config.CurrentUserArgumentResolver;
import com.example.chatservice.user.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@ContextConfiguration(classes = {ChatController.class, ChatControllerTest.TestConfig.class})
class ChatControllerTest {

    @Configuration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new CurrentUserArgumentResolver());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        testUserPrincipal = new UserPrincipal(1L, "testUser");
    }

    // todo : 질문 -> userId
    @Test
    @DisplayName("일대일 채팅방을 생성한다")
    void createDirectChatRoom() throws Exception {
        // given
        ChatRequest chatRequest = createChatRequest(List.of(2L));
        ChatResponse expectedResponse = createChatResponse(1L, ChatType.DIRECT);

        given(chatService.createChatRoom(any(Long.class), any(ChatRequest.class)))
                .willReturn(expectedResponse);

        // when & then
        mockMvc.perform(post("/api/v1/chat")
                        .requestAttr("userPrincipal", testUserPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.type").value("DIRECT"));
    }

    @Test
    @DisplayName("그룹 채팅방을 생성한다")
    void createGroupChatRoom() throws Exception {
        // given
        ChatRequest chatRequest = createChatRequest(List.of(2L, 3L));
        ChatResponse expectedResponse = createChatResponse(1L, ChatType.GROUP);

        given(chatService.createChatRoom(any(Long.class), any(ChatRequest.class)))
                .willReturn(expectedResponse);

        // when & then
        mockMvc.perform(post("/api/v1/chat")
                        .requestAttr("userPrincipal", testUserPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.type").value("GROUP"));
    }

    @Test
    @DisplayName("셀프 채팅방을 생성한다")
    void createImChatRoom() throws Exception {
        // given
        ChatRequest chatRequest = createChatRequest(List.of(1L));
        ChatResponse expectedResponse = createChatResponse(1L, ChatType.IM);

        given(chatService.createChatRoom(any(Long.class), any(ChatRequest.class)))
                .willReturn(expectedResponse);

        // when & then
        mockMvc.perform(post("/api/v1/chat")
                        .requestAttr("userPrincipal", testUserPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.type").value("IM"));
    }


    @Test
    @DisplayName("내 채팅방 목록을 조회할 수 있다.")
    void getChatRooms() throws Exception {
        // given
        List<ChatRoomResponse> expectedResponse = List.of(
                ChatRoomResponse.builder()
                        .chatRoomId(1L)
                        .lastMessage("안녕하세요")
                        .lastMessageDateTime(LocalDateTime.now())
                        .unreadCount(2L)
                        .lastMessageId(10L)
                        .build()
        );

        given(chatService.getChatRooms(1L))
                .willReturn(expectedResponse);

        // when & then
        mockMvc.perform(
                        get("/api/v1/chats")
                                .requestAttr("userPrincipal", testUserPrincipal)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].lastMessage").value("안녕하세요"));

        verify(chatService).getChatRooms(1L);
    }

    @Test
    @DisplayName("채팅방에 가입할 수 있다.")
    void joinChat() throws Exception {
        // given
        Long chatRoomId = 1L;

        // when & then
        mockMvc.perform(post("/api/v1/chat/{chatId}", chatRoomId)
                        .requestAttr("userPrincipal", testUserPrincipal))
                .andExpect(status().isOk());

        verify(chatService).joinChatRoom(chatRoomId, testUserPrincipal.getId());
    }

    @Test
    @DisplayName("채팅방에서 나갈 수 있다.")
    void leaveChat() throws Exception {
        // given
        Long chatRoomId = 1L;

        // when & then
        mockMvc.perform(
                        delete("/api/v1/chat/{chatRoomId}", chatRoomId)
                                .requestAttr("userPrincipal", testUserPrincipal))
                .andExpect(status().isOk());

        verify(chatService).leaveChatRoom(chatRoomId, testUserPrincipal.getId());
    }

    private ChatResponse createChatResponse(Long chatId, ChatType chatType) {
        return ChatResponse.builder()
                .id(chatId)
                .type(chatType)
                .build();
    }

    private ChatRequest createChatRequest(List<Long> userIds) {
        return ChatRequest.builder()
                .userIds(userIds)
                .build();
    }
}