package com.example.chatservice.common.ws;

import com.example.chatservice.common.SessionManager;
import com.example.chatservice.message.service.MessageService;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandler extends TextWebSocketHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Long userId = extractUserId(session);
            Long chatRoomId = extractChatRoomId(session);

            // Session에 userId, chatRoomId 저장
            session.getAttributes().put("userId", userId);
            if (chatRoomId != null) {
                session.getAttributes().put("chatRoomId", chatRoomId);
                log.info("WebSocket connected! userId={}, chatRoomId={}, sessionId={}", userId, chatRoomId, session.getId());
            } else {
                log.info("WebSocket connected! userId={} (no chatRoomId), sessionId={}", userId, session.getId());
            }

            // SessionManager에 등록 (Redis에도 저장됨)
            sessionManager.addSession(userId, session);
        } catch (IllegalArgumentException e) {
            log.error("Invalid WebSocket connection parameters: {}", e.getMessage());
            session.close(CloseStatus.BAD_DATA.withReason("Invalid parameters: " + e.getMessage()));
            throw e;
        } catch (Exception e) {
            log.error("Error establishing WebSocket connection: {}", e.getMessage(), e);
            try {
                session.close(CloseStatus.SERVER_ERROR.withReason("Internal server error: " + e.getMessage()));
            } catch (Exception closeException) {
                log.error("Failed to close WebSocket session", closeException);
            }
            throw e;
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = (Long) session.getAttributes().get("userId");
        String payload = message.getPayload();

        log.info("Received message from user {}: {}", senderId, payload);

        try {
            // chatRoomId는 세션에서 먼저 확인
            Long chatRoomId = (Long) session.getAttributes().get("chatRoomId");
            String content;
            Long receiverId = null;
            
            // JSON 파싱 시도
            try {
                Map<String, Object> messageData = objectMapper.readValue(payload, Map.class);
                
                // content 추출
                if (messageData.containsKey("content")) {
                    content = messageData.get("content").toString();
                } else {
                    // content가 없으면 전체 payload를 content로 사용
                    content = payload;
                }
                
                // chatRoomId는 메시지에서도 받을 수 있음 (세션에 없을 때만)
                if (chatRoomId == null && messageData.containsKey("chatRoomId") && messageData.get("chatRoomId") != null) {
                    chatRoomId = Long.valueOf(messageData.get("chatRoomId").toString());
                    // 세션에 저장 (다음 메시지부터 사용)
                    session.getAttributes().put("chatRoomId", chatRoomId);
                }
                
                // receiverId는 선택적 (1:1 채팅일 때만 필요, 그룹 채팅은 null)
                if (messageData.containsKey("receiverId") && messageData.get("receiverId") != null) {
                    receiverId = Long.valueOf(messageData.get("receiverId").toString());
                }
                
            } catch (JsonParseException e) {
                // JSON이 아닌 경우 - 일반 텍스트 메시지로 처리
                log.info("Received plain text message from user {}: {}", senderId, payload);
                content = payload;
            }
            
            // chatRoomId 확인
            if (chatRoomId == null) {
                String errorMsg = "{\"error\": \"chatRoomId is required. Connect with ?chatRoomId=X or include in first message\"}";
                session.sendMessage(new TextMessage(errorMsg));
                log.error("chatRoomId not found for user {}", senderId);
                return;
            }

            //1번
            messageService.sendMessageViaWebSocket(senderId, receiverId, chatRoomId, content);

        } catch (Exception e) {
            log.error("Error processing message from user {}", senderId, e);
            session.sendMessage(new TextMessage("{\"error\": \"Failed to process message\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");

        if (userId != null) {
            sessionManager.removeSession(userId);
            log.info("WebSocket disconnected! userId={}", userId);
        }
    }

    private Long extractUserId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) {
                throw new IllegalArgumentException("WebSocket URI is null");
            }
            
            String query = uri.getQuery(); // "userId=1&chatRoomId=2" or "userId=1"

            if (query != null && !query.isEmpty()) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("userId=")) {
                        String userIdStr = param.substring("userId=".length());
                        if (userIdStr.isEmpty()) {
                            throw new IllegalArgumentException("userId value is empty");
                        }
                        return Long.parseLong(userIdStr);
                    }
                }
            }

            throw new IllegalArgumentException("userId is required in query parameter. Format: ws://host/chat?userId=X&chatRoomId=Y");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("userId must be a valid number", e);
        }
    }
    
    private Long extractChatRoomId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            if (uri == null) {
                return null;
            }
            
            String query = uri.getQuery(); // "userId=1&chatRoomId=2"

            if (query != null && !query.isEmpty()) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("chatRoomId=")) {
                        String chatRoomIdStr = param.substring("chatRoomId=".length());
                        if (chatRoomIdStr.isEmpty()) {
                            return null;
                        }
                        return Long.parseLong(chatRoomIdStr);
                    }
                }
            }

            return null; // chatRoomId는 선택적
        } catch (NumberFormatException e) {
            log.warn("Invalid chatRoomId format, ignoring: {}", e.getMessage());
            return null; // chatRoomId는 선택적이므로 예외를 던지지 않음
        }
    }
}