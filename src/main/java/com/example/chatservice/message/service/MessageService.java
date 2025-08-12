package com.example.chatservice.message.service;

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

    @Transactional
    public void sendMessage(MessageRequest messageRequest, Long userId, Long receiverId) {

        User user = userRepository.findById(userId).orElseThrow();
        User receiverUser = userRepository.findById(receiverId).orElseThrow();

        Message message = Message.builder()
                .sender(user)
                .receiver(receiverUser)
                .message(messageRequest.getMessage())
                .build();

        messageRepository.save(message);
    }

    public List<MessageResponse> getMessages(Long userId) {
        List<MessageResponse> messageResponses = new ArrayList<>();

        List<Message> messages = messageRepository.findAllByReceiverId(userId);

        for (Message message : messages) {
            MessageResponse messageResponse = new MessageResponse(message.getMessage());
            messageResponses.add(messageResponse);
        }

        return messageResponses;
    }
}
