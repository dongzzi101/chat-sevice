package com.example.chatservice.chat.sercivce;

import com.example.chatservice.chat.controller.request.ChatRequest;
import com.example.chatservice.chat.controller.request.ChatResponse;
import com.example.chatservice.chat.controller.request.ChatRoomResponse;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.entity.ChatType;
import com.example.chatservice.chat.entity.ReadStatus;
import com.example.chatservice.chat.entity.UserChat;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.chat.repository.ReadStatusRepository;
import com.example.chatservice.chat.repository.UserChatRepository;
import com.example.chatservice.exception.ChatRoomNotFoundException;
import com.example.chatservice.exception.UserNotJoinedException;
import com.example.chatservice.exception.UserNotFoundException;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import com.sun.jdi.InvalidCodeIndexException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserChatRepository userChatRepository;
    private final ReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ChatResponse createChatRoom(Long currentUserId, ChatRequest chatRequest) {

        List<Long> userIds = new ArrayList<>(chatRequest.getUserIds());
        userIds.add(currentUserId);
        List<Long> normalizedUserIds = normalizeUserIds(userIds);

        ChatRoom chatRoom;

        // TODO:FLOW - 3. 채팅방이 있는지 확인
        ChatRoom existChatRoom = findExistChatRoom(normalizedUserIds);

        // TODO:FLOW - 3.1. 있는 경우 기존 채팅방
        if (existChatRoom != null) {
            chatRoom = existChatRoom;

        } else {

            // TODO:FLOW - 3.2. 채팅방 없는 경우 새로운 채팅방을 생성

            String chatKey = createChatKey(normalizedUserIds);

            boolean isSelfChat = normalizedUserIds.size() == 1;

            ChatType chatType = isSelfChat ? ChatType.IM :
                    (normalizedUserIds.size() == 2 ? ChatType.DIRECT : ChatType.GROUP);

            chatRoom = ChatRoom.builder()
                    .type(chatType)
                    .chatKey(chatKey)
                    .build();

            chatRepository.save(chatRoom);

            Set<Long> uniqueUserIds = new HashSet<>(normalizedUserIds);

            for (Long userId : uniqueUserIds) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

                UserChat userChat = UserChat.builder()
                        .chatRoom(chatRoom)
                        .user(user)
                        .build();
                userChatRepository.save(userChat);

                // TODO:FLOW - 4.채팅방 생성 시 read_status 생성
                ReadStatus readStatus = ReadStatus.builder()
                        .user(user)
                        .chatRoom(chatRoom)
                        .lastReadMessage(null)
                        .build();
                readStatusRepository.save(readStatus);
            }

        }

        return new ChatResponse(chatRoom.getId(), chatRoom.getType());
    }

    private ChatRoom findExistChatRoom(List<Long> userIds) {
        String chatKey = createChatKey(userIds);
        return chatRepository.findChatRoomByChatKey(chatKey).orElse(null);
    }

    // chat key 생성해주는 역할
    private String createChatKey(List<Long> userIds) {
        try {
            // 1. 정렬 + 중복 제거 + 문자열 결합
            String rawKey = normalizeUserIds(userIds).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining("_"));

            // 2. SHA-256 해시
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));

            // 3. hex 문자열 변환
            return bytesToHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private List<Long> normalizeUserIds(List<Long> userIds) {
        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }


    public List<ChatRoomResponse> getChatRooms(Long currentUserId) {

        List<ChatRoomResponse> chatroomResponses = new ArrayList<>();

        List<UserChat> userChatList = userChatRepository.findByUserIdOrderByLastMessageIdDesc(currentUserId);
        User currentUser = userRepository.findById(currentUserId).orElseThrow();

        List<Long> lastMessageIds = userChatList.stream()
                .map(UserChat::getLastMessageId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Message> lastMessagesById = messageRepository.findAllById(lastMessageIds)
                .stream()
                .collect(Collectors.toMap(Message::getId, m -> m));

        for (UserChat userChat : userChatList) {

            ChatRoom chatRoom = userChat.getChatRoom();
            // 1. 채팅방의 마지막 메시지 가져오기
            Message lastMessage = userChat.getLastMessageId() != null
                    ? lastMessagesById.get(userChat.getLastMessageId())
                    : null;

            if (lastMessage == null && userChat.getLastMessageId() == null) {
                lastMessage = messageRepository
                        .findTopByChatRoomIdOrderByIdDesc(chatRoom.getId())
                        .orElse(null);
            }

            // 2. 내가 마지막으로 읽은 메시지 정보 가져오기 -> 안읽은 메시지 수 개산하려고
            ReadStatus myReadStatus = readStatusRepository
                    .findByUserAndChatRoom(currentUser, chatRoom);

            // 3. 안읽은 메시지 개수 계산
            long unreadCount = 0;
            if (myReadStatus != null && myReadStatus.getLastReadMessage() != null) {
                // 내가 읽은 메시지 이후의 메시지 개수
                unreadCount = messageRepository.countByChatRoomIdAndIdGreaterThan(
                        chatRoom.getId(),
                        myReadStatus.getLastReadMessage().getId()
                );
            } else if (lastMessage != null) {
                // ReadStatus가 없거나 한 번도 안 읽었으면 전체 메시지가 안읽음
                unreadCount = messageRepository.countByChatRoomId(chatRoom.getId());
            }

            ChatRoomResponse response = ChatRoomResponse.builder()
                    .chatRoomId(chatRoom.getId())
                    .lastMessage(lastMessage != null ? lastMessage.getMessage() : null)
                    .lastMessageDateTime(lastMessage != null ? lastMessage.getCreatedAt() : null)
                    .lastMessageId(lastMessage != null ? lastMessage.getId() : null)
                    .unreadCount(unreadCount)
                    .build();

            chatroomResponses.add(response);
        }

        return chatroomResponses;
    }

    @Transactional
    public void joinChatRoom(Long chatId, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        ChatRoom chatRoom = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatId));
        // 코드의 가독성을 좋게하기위해 예외케이스는 빨리빨리 던져버린다.
        // 이렇게 던진 에러(익셉션)을 따로 핸들링 해줘야하는가? 아닌가

        // 계좌의 잔액이 부족합니다
        // new 계좌잔액부족Exception();

        UserChat userChat = UserChat
                .builder()
                .user(user)
                .chatRoom(chatRoom)
                .build();

        userChatRepository.save(userChat);

        // ReadStatus도 생성 (채팅방 참여 시 읽음 상태 초기화)
        ReadStatus existingReadStatus = readStatusRepository.findByUserAndChatRoom(user, chatRoom);
        if (existingReadStatus == null) {
            ReadStatus readStatus = ReadStatus.builder()
                    .user(user)
                    .chatRoom(chatRoom)
                    .lastReadMessage(null)
                    .build();
            readStatusRepository.save(readStatus);
        }
    }

    // @ExceptionHandler(RuntimeException.class)
    // public String test(){}

    @Transactional
    public void leaveChatRoom(Long chatRoomId, Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));
        ChatRoom chatRoom = chatRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        UserChat userChat = userChatRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new UserNotJoinedException(chatRoomId, currentUserId));

        userChat.leaveChatRoom();

        // TODO 여기부분 공부
        List<UserChat> activeUserChats = chatRoom.getUserChats().stream()
                .filter(uc -> uc.getLeavedAt() == null)
                .toList();

        Set<Long> activeUserIds = activeUserChats.stream()
                .map(uc -> uc.getUser().getId())
                .collect(Collectors.toSet());

        String chatKey = createChatKey(new ArrayList<>(activeUserIds));
        chatRoom.updateChatKey(chatKey);

    }
}


/*
1. userchat paging 30개? (제일 최근에 메시지가 발생한 채팅방부터 30개) (최근에 가장 구매가 많았던 상품부터 30개) (별도의 정렬조건들...)
createad id ...

2. 해당하는 message 조회(지금은 단건씨 조회하지만, 제가 배치로 묶어서 하자... 성능)

3. message time 보고 user chat을 다시 내가 원하는대로 정렬해준다...
* */

/*
11
10
9
8
7
6
5
4
3
2
1
select * userchat order by id desc limit 10;

select * messages in (2~11)


*/