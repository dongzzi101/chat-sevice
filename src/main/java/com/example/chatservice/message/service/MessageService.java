package com.example.chatservice.message.service;

import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.entity.Message;
import com.example.chatservice.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    @Transactional
    public void sendMessage(MessageRequest messageRequest) {

        Message message = Message.builder()
                .message(messageRequest.getMessage())
                .build();

        messageRepository.save(message);
    }

    public List<MessageResponse> getMessages() {
        List<MessageResponse> messageResponses = new ArrayList<>();

        List<Message> messages = messageRepository.findAll();

        for (Message message : messages) {
            MessageResponse messageResponse = new MessageResponse(message.getMessage());
            messageResponses.add(messageResponse);
        }

        return messageResponses;
    }
}
