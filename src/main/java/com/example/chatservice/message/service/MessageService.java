package com.example.chatservice.message.service;

import com.example.chatservice.chat.entity.Chat;
import com.example.chatservice.chat.repository.ChatRepository;
import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import com.example.chatservice.user.entity.User;
import com.example.chatservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    @Transactional
    public void sendMessage(MessageRequest messageRequest, Long senderUserId, Long chatRoomId) {
        User senderUser = userRepository.findById(senderUserId).orElseThrow();
        Chat chatRoom = chatRepository.findById(chatRoomId).orElseThrow();

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
                .sender(senderUser)
                .chatRoom(chatRoom)
                .message(messageRequest.getMessage())
                .build();

        messageRepository.save(message);
    }

    public List<MessageResponse> getMessages(Long currentUserId, Long chatRoomId) {
        List<MessageResponse> messageResponses = new ArrayList<>();

        List<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId);

        for (Message message : messages) {
            MessageResponse messageResponse = new MessageResponse(message.getSender().getId(), message.getMessage());
            messageResponses.add(messageResponse);
        }

        return messageResponses;
    }
}
