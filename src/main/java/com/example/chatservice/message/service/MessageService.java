package com.example.chatservice.message.service;

import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ReadStatus;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ReadStatusRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.chat.service.ReadStatusService;
import com.example.chatservice.component.snowflake.Snowflake;
import com.example.chatservice.exception.ChatRoomNotFoundException;
import com.example.chatservice.exception.UserNotFoundException;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.event.ChatMessageEvent;
import com.example.chatservice.message.kafka.ChatMessageProducer;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.sharding.Sharding;
import com.example.chatservice.sharding.ShardingTarget;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ReadStatusRepository readStatusRepository;
    private final UserChatRepository userChatRepository;
    private final ReadStatusService readStatusService;
    private final Snowflake snowflake;
    private final MessageDeliveryService messageDeliveryService; // 동기 전송용
    private final ChatMessageProducer chatMessageProducer;
    private final PendingLastMessageFlushService pendingLastMessageFlushService;
    private final HotRoomDetectionService hotRoomDetectionService;
    private final TransactionTemplate transactionTemplate; // primary TM (main DB)

    /** ChainedTransactionManager: Main + Message 한 트랜잭션으로 묶음 (순서대로 시작, 역순 커밋) */
    @Sharding(target = ShardingTarget.MESSAGE, key = "#chatRoomId")
    @Transactional(transactionManager = "chainedTransactionManager")
    public void sendMessageViaWebSocket(Long senderId, Long receiverId,
                                        Long chatRoomId, String content) {

        User senderUser = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        Message message = Message.builder()
                .id(snowflake.nextId())
                .senderId(senderUser.getId())
                .chatRoomId(chatRoom.getId())
                .message(content)
                .build();

        messageRepository.saveAndFlush(message);

        // 같은 chained tx 안에서 Main DB 작업
        performUserChatLastMessageUpdate(chatRoom, message.getId());
        performSenderReadStatusUpdate(senderUser.getId(), chatRoom.getId(), message.getId());

        log.info("Sending message to sender {} immediately", senderId);
        messageDeliveryService.deliverMessage(senderId, message);

        Long messageId = message.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ChatMessageEvent event = new ChatMessageEvent(
                                message.getId(),
                                message.getSenderId(),
                                message.getMessage(),
                                message.getChatRoomId(),
                                message.getCreatedAt()
                        );
                        chatMessageProducer.sendMessage(event);
                    }
                }
        );
    }

    /** Main DB 업데이트 핵심 로직 (호출 시점의 트랜잭션에서 실행) */
    private void performUserChatLastMessageUpdate(ChatRoom chatRoom, Long messageId) {
        boolean hotRoom = hotRoomDetectionService.isHotRoom(chatRoom.getId());

        if (hotRoom && hotRoomDetectionService.shouldSkipHotUpdate(chatRoom.getId())) {
            pendingLastMessageFlushService.scheduleFlush(
                    chatRoom.getId(),
                    messageId,
                    hotRoomDetectionService.getDebounceDuration()
            );
            return;
        }
        pendingLastMessageFlushService.flushIfPending(chatRoom.getId());
        userChatRepository.updateLastMessageIdForChat(chatRoom.getId(), messageId);
    }

    /** Main DB만 쓸 때 사용 (별도 tx) */
    private void updateUserChatLastMessage(ChatRoom chatRoom, Long messageId) {
        transactionTemplate.executeWithoutResult(status ->
                performUserChatLastMessageUpdate(chatRoom, messageId)
        );
    }

    /** ReadStatus 업데이트 핵심 로직 (호출 시점의 트랜잭션에서 실행) */
    private void performSenderReadStatusUpdate(Long senderId, Long chatRoomId, Long messageId) {
        User senderUser = userRepository.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException(senderId));
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        ReadStatus senderReadStatus = readStatusService.getOrCreateReadStatus(senderUser, chatRoom);
        Long currentLastRead = senderReadStatus.getLastReadMessageId();
        if (currentLastRead == null || messageId > currentLastRead) {
            senderReadStatus.updateReadMessage(messageId);
        }
    }

    /** Main DB만 쓸 때 사용 (별도 tx) */
    private void updateSenderReadStatus(Long senderId, Long chatRoomId, Long messageId) {
        transactionTemplate.executeWithoutResult(status ->
                performSenderReadStatusUpdate(senderId, chatRoomId, messageId)
        );
    }

    @Sharding(target = ShardingTarget.MESSAGE, key = "#chatRoomId")
    @Transactional(readOnly = true, transactionManager = "messageTransactionManager")
    public List<MessageResponse> getMessages(
            Long currentUserId,
            Long chatRoomId,
            Long lastReadMessageId,
            int before, int after
    ) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        // 1. lastReadMessageId가 null이면 DB에서 내 ReadStatus 조회
        if (lastReadMessageId == null) {
            ReadStatus myReadStatus = readStatusRepository
                    .findByUserAndChatRoom(currentUser, chatRoom);

            if (myReadStatus != null && myReadStatus.getLastReadMessageId() != null) {
                // 내가 마지막으로 읽은 메시지 기준
                lastReadMessageId = myReadStatus.getLastReadMessageId();
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
                    message.getSenderId(),
                    message.getMessage()
            ));
        }

        return messageResponses;
    }

    /**
     * 메시지 읽음 처리 (ChainedTransactionManager: Message 조회 + ReadStatus 업데이트 한 tx)
     */
    @Sharding(target = ShardingTarget.MESSAGE, key = "#chatRoomId")
    @Transactional(transactionManager = "chainedTransactionManager")
    public void markMessagesAsRead(Long currentUserId, Long chatRoomId, Long messageId) {
        Message targetMessage;
        if (messageId != null) {
            targetMessage = messageRepository.findById(messageId).orElse(null);
            if (targetMessage == null) {
                log.warn("Message not found for ACK: messageId={}, userId={}, chatRoomId={}. " +
                        "This may happen if messageId is incorrect or message was deleted.",
                        messageId, currentUserId, chatRoomId);
                return;
            }
        } else {
            targetMessage = messageRepository
                    .findTopByChatRoomIdOrderByIdDesc(chatRoomId)
                    .orElse(null);
        }

        if (targetMessage == null) {
            return;
        }

        performReadStatusUpdate(currentUserId, chatRoomId, targetMessage.getId());
    }

    /** ReadStatus 업데이트 핵심 로직 (호출 시점의 트랜잭션에서 실행) */
    private void performReadStatusUpdate(Long userId, Long chatRoomId, Long messageId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        ReadStatus userReadStatus = readStatusService.getOrCreateReadStatus(user, chatRoom);
        Long currentLastRead = userReadStatus.getLastReadMessageId();
        if (currentLastRead == null || messageId > currentLastRead) {
            userReadStatus.updateReadMessage(messageId);
        }
    }

    /** Main DB만 쓸 때 사용 (별도 tx) */
    private void updateReadStatus(Long userId, Long chatRoomId, Long messageId) {
        transactionTemplate.executeWithoutResult(status ->
                performReadStatusUpdate(userId, chatRoomId, messageId)
        );
    }
}
