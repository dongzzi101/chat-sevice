package com.example.chatservice.message.service;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ReadStatus;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ReadStatusRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.common.ServerInfoProvider;
import com.example.chatservice.common.SessionManager;
import com.example.chatservice.common.snowflake.Snowflake;
import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final RedisTemplate<String, String> redisTemplate;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UserChatRepository userChatRepository;
    private final Snowflake snowflake = new Snowflake();
    private final ServerInfoProvider serverInfoProvider;
    private final SessionManager sessionManager;
    private final RestTemplate restTemplate;
    private final AsyncMessageDeliveryService asyncMessageDeliveryService;
    private final MessageDeliveryService messageDeliveryService; // 동기 전송용


    @Transactional
    public void sendMessage(MessageRequest messageRequest, Long senderUserId, Long chatRoomId) {
        User senderUser = userRepository.findById(senderUserId).orElseThrow();
        ChatRoom chatRoom = chatRepository.findById(chatRoomId).orElseThrow();

        // TODO 3 : 근데 메시지 저장이 하나만 되는게 맞겠죠? 중요도 낮음
        /**
         * userA -> userB :hello
         * message : hello 저장
         * userB는 메시지를 어떻게 가져올까 고민
         * 나중에 메시지가 너무 많아지면 userId를 기준으로 샤드키로 잡고 샤딩을 하는건가..?
         * 근데 메시지를 userId로 샤드키로 해두면 사용하는 의미가 잇나?
         * 뭘로해야하는지 고민
         */

        Message message = Message.builder()
                .id(snowflake.nextId())
                .sender(senderUser)
                .chatRoom(chatRoom)
                .message(messageRequest.getMessage())
                .build();

        //TODO:FLOW - 6. 메시지 전송 후 read_status 상태를 업데이트 해야 함
        ReadStatus senderReadStatus = readStatusRepository.findByUserAndChatRoom(senderUser, chatRoom);
        
        // ReadStatus가 없으면 생성 (joinChatRoom으로 추가된 사용자는 ReadStatus가 없을 수 있음)
        if (senderReadStatus == null) {
            senderReadStatus = ReadStatus.builder()
                    .user(senderUser)
                    .chatRoom(chatRoom)
                    .lastReadMessage(null)
                    .build();
            readStatusRepository.save(senderReadStatus);
        }

         messageRepository.saveAndFlush(message); // flush 필수
//        messageRepository.save(message);

        senderReadStatus.updateReadMessage(message);
    }

    @Transactional
    public void sendMessageViaWebSocket(Long senderId, Long receiverId,
                                        Long chatRoomId, String content) {
        log.info("Sending message from {} in chatRoom {}", senderId, chatRoomId);

        // 1. User, ChatRoom 조회
        User senderUser = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));

        // 2. DB에 메시지 저장
        Message message = Message.builder()
                .id(snowflake.nextId())
                .sender(senderUser)
                .chatRoom(chatRoom)
                .message(content)
                .build();

        messageRepository.saveAndFlush(message);
        log.info("Message saved to DB: id={}", message.getId());

        // 3. ReadStatus 업데이트 (발신자)
        ReadStatus senderReadStatus = readStatusRepository.findByUserAndChatRoom(senderUser, chatRoom);
        senderReadStatus.updateReadMessage(message);

        // 4. 발신자 본인에게 즉시 전송 (동기) - UX를 위해
        log.info("Sending message to sender {} immediately", senderId);
        messageDeliveryService.deliverMessage(senderId, message);

        // 엔티티 꼭 찝어서 가져와서 업데이트
        // vs
        // @Modifiying. ... update ...
        // update last seen ... last seen message id = 10 where last seen message id < 10;    // 11
        // 낙관적락

        // 5. 다른 사용자들에게는 비동기로 전송
        // 5. 트랜잭션 커밋 후 비동기 실행
        Long messageId = message.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Transaction committed. Starting async delivery for message {}", messageId);
                        asyncMessageDeliveryService.deliverMessageAsync(
                                senderId,
                                receiverId,
                                chatRoomId,
                                messageId
                        );
                    }
                }
        );
//        MessageDto.from(message);
    }


    /*@Transactional
    public void sendMessageViaWebSocket(Long senderId, Long receiverId, Long chatRoomId, String content) {
        log.info("Sending message from {} in chatRoom {}", senderId, chatRoomId);

        // 1. User, ChatRoom 조회
        User senderUser = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found"));

        // 2. DB에 메시지 저장
        Message message = Message.builder()
                .id(snowflake.nextId())
                .sender(senderUser)
                .chatRoom(chatRoom)
                .message(content)
                .build();

        messageRepository.saveAndFlush(message);
        log.info("Message saved to DB: id={}", message.getId());

        // 3. ReadStatus 업데이트 (발신자)
        ReadStatus senderReadStatus = readStatusRepository.findByUserAndChatRoom(senderUser, chatRoom);
        senderReadStatus.updateReadMessage(message);

        // 4. 실시간 전송
        // receiverId가 있으면 1:1 채팅, 없으면 그룹 채팅 (채팅방 참여자 모두에게 전송)
        if (receiverId != null) {
            // 1:1 채팅 - 특정 수신자에게만 전송
            deliverMessage(receiverId, message);
        } else {
            // 그룹 채팅 - 채팅방 참여자 모두에게 전송 (본인 제외)
            deliverMessageToChatRoom(senderId, chatRoom, message);
        }
    }*/
    
    /*private void deliverMessageToChatRoom(Long senderId, ChatRoom chatRoom, Message message) {
        // 채팅방에 참여한 모든 유저 조회 (나간 유저 제외)
        List<UserChat> userChats = userChatRepository.findByChatRoomIdAndLeavedAtIsNull(chatRoom.getId());
        
        log.info("Delivering message to {} participants in chatRoom {}", userChats.size(), chatRoom.getId());
        
        for (UserChat userChat : userChats) {
            Long participantId = userChat.getUser().getId();
            
            // 본인은 제외
            if (participantId.equals(senderId)) {
                log.info("Skipping sender {} (self)", participantId);
                continue;
            }
            
            log.info("Attempting to deliver message to participant {}", participantId);
            // 각 참여자에게 메시지 전송
            deliverMessage(participantId, message);
        }
    }

    private void deliverMessage(Long receiverId, Message message) {
        // Redis에서 수신자 서버 찾기
        String redisKey = "user:" + receiverId;
        String targetServer = redisTemplate.opsForValue().get(redisKey);

        log.info("Looking up user {} in Redis (key: {}), found server: {}", receiverId, redisKey, targetServer);

        if (targetServer == null) {
            log.warn("⚠User {} is offline (not found in Redis)", receiverId);
            return;
        }

        // 현재 서버 주소
        String currentServer = serverInfoProvider.getServerAddress();
        log.info("Current server: {}, Target server: {}", currentServer, targetServer);

        // 같은 서버에 있으면 바로 전송
        if (targetServer.equals(currentServer)) {
            log.info("User {} is on the same server. Sending directly.", receiverId);

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", message.getId());
            messageData.put("senderId", message.getSender().getId());
            messageData.put("content", message.getMessage());
            messageData.put("chatRoomId", message.getChatRoom().getId());
            messageData.put("sentAt", message.getCreatedAt());

            sessionManager.sendToUser(receiverId, messageData);
            log.info("Message sent directly to user {}", receiverId);
        } else {
            // 다른 서버로 HTTP 전송
            log.info("User {} is on different server {}. Forwarding via HTTP.", receiverId, targetServer);
            forwardToOtherServer(targetServer, receiverId, message);
        }
    }

    private void forwardToOtherServer(String targetServer, Long receiverId, Message message) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("receiverId", receiverId);
            request.put("messageId", message.getId());
            request.put("senderId", message.getSender().getId());
            request.put("content", message.getMessage());
            request.put("chatRoomId", message.getChatRoom().getId());
            request.put("sentAt", message.getCreatedAt());

            String url = "http://" + targetServer + "/internal/message";
            log.info("Forwarding message to user {} via HTTP: {}", receiverId, url);
            log.info("Request payload: {}", request);

            restTemplate.postForObject(url, request, Void.class);
            log.info("Message forwarded successfully to {} for user {}", targetServer, receiverId);

        } catch (Exception e) {
            log.error("Failed to forward message to {} for user {}", targetServer, receiverId, e);
            log.error("Exception details: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {}", e.getCause().getMessage());
            }
        }
    }*/

        @Transactional
        public List<MessageResponse> getMessages (
                Long currentUserId,
                Long chatRoomId,
                Long lastReadMessageId,
        int before, int after
    ){
            User currentUser = userRepository.findById(currentUserId).orElseThrow();
            ChatRoom chatRoom = chatRepository.findById(chatRoomId).orElseThrow();

            // 1. lastReadMessageId가 null이면 DB에서 내 ReadStatus 조회
            if (lastReadMessageId == null) {
                ReadStatus myReadStatus = readStatusRepository
                        .findByUserAndChatRoom(currentUser, chatRoom);

                if (myReadStatus != null && myReadStatus.getLastReadMessage() != null) {
                    // 내가 마지막으로 읽은 메시지 기준
                    lastReadMessageId = myReadStatus.getLastReadMessage().getId();
                } else {
                    // 처음 들어온 사람이면 첫 메시지부터
                    Message firstMessage = messageRepository
                            .findTopByChatRoomIdOrderByIdAsc(chatRoomId)
                            .orElse(null);

                    if (firstMessage != null) {
                        lastReadMessageId = firstMessage.getId();
                        before = 0;  // 처음이니까 before는 0
                    } else {
                        // 메시지가 하나도 없으면 빈 리스트 반환
                        return new ArrayList<>();
                    }
                }
            }

            // 2. before 메시지 조회 (lastReadMessageId보다 작은 ID)
            List<Message> beforeMessages = new ArrayList<>();
            if (before > 0) {
                beforeMessages = messageRepository
                        .findByChatRoomIdAndIdLessThanOrderByIdDesc(
                                chatRoomId,
                                lastReadMessageId,
                                PageRequest.of(0, before)
                        );
            }

            // 3. after 메시지 조회 (lastReadMessageId보다 큰 ID)
            List<Message> afterMessages = new ArrayList<>();
            if (after > 0) {
                afterMessages = messageRepository
                        .findByChatRoomIdAndIdGreaterThanOrderByIdAsc(
                                chatRoomId,
                                lastReadMessageId,
                                PageRequest.of(0, after)
                        );
            }

            // 4. baseMessage 자체도 포함
            Message baseMessage = messageRepository.findById(lastReadMessageId).orElse(null);

            // 5. 합치기 (시간순 정렬)
            List<Message> allMessages = new ArrayList<>();
            Collections.reverse(beforeMessages); // DESC였으니 뒤집기
            allMessages.addAll(beforeMessages);
            if (baseMessage != null) {
                allMessages.add(baseMessage);
            }
            allMessages.addAll(afterMessages);

            // 6. 읽음 처리: 채팅방의 최신 메시지로 업데이트
            Message latestMessageInChatRoom = messageRepository
                    .findTopByChatRoomIdOrderByIdDesc(chatRoomId)
                    .orElse(null);

            if (latestMessageInChatRoom != null) {
                ReadStatus userReadStatus = readStatusRepository
                        .findByUserAndChatRoom(currentUser, chatRoom);

                // 현재 읽은 메시지보다 최신이면 업데이트
                if (userReadStatus.getLastReadMessage() == null ||
                        latestMessageInChatRoom.getId() > userReadStatus.getLastReadMessage().getId()) {
                    userReadStatus.updateReadMessage(latestMessageInChatRoom);
                }
            }

            // 6.
        /*
        1. 해당 채팅방에 참여중인 유저아이디들을 조회. (리스트 롱)
        2. 반복문 돌리면서
            2-1. 해당유저아이디의 웹소켓 세션을 찾는다.
            2-2. 해당 웹소켓 세션으로 새로운 메시지를 전송해준다.
            2-3. 웹소켓세션에 없으면 레디스에서 찾아서 해당서버로 요청을 보내준다.
            2-4. 레디스에도 없으면 우리서버에 접속중이지 않으므로 fcm 전송
        3. 모든 유저들에게 실시간으로 새로운 메시지 전달 완료.

        */

            // 7. Response 변환
            List<MessageResponse> messageResponses = new ArrayList<>();
            for (Message message : allMessages) {
                messageResponses.add(new MessageResponse(
                        message.getSender().getId(),
                        message.getMessage()
                ));
            }

            return messageResponses;
        }

//    @Transactional
//    public List<MessageResponse> getMessages(
//            Long currentUserId,
//            Long chatRoomId,
//            Long lastReadMessageId,
//            int before, int after
//    ) {
//        User currentUser = userRepository.findById(currentUserId).orElseThrow();
//        ChatRoom chatRoom = chatRepository.findById(chatRoomId).orElseThrow();
//
//        List<Message> messages = messageRepository
//                .findByChatRoomIdOrderByCreatedAtDesc(chatRoomId);
//
//        //TODO:FLOW - 7. 메시지를 읽으면 read_status 어디까지 읽었는지 업데이트
//        if (!messages.isEmpty()) {
//            Message latestMessage = messages.get(0);
//
//            ReadStatus userReadStatus = readStatusRepository
//                    .findByUserAndChatRoom(currentUser, chatRoom);
//
//            userReadStatus.updateReadMessage(latestMessage);
//        }
//
//        List<MessageResponse> messageResponses = new ArrayList<>();
//        for (Message message : messages) {
//            messageResponses.add(new MessageResponse(
//                    message.getSender().getId(),
//                    message.getMessage()
//            ));
//        }
//
//        return messageResponses;
//    }
    }
