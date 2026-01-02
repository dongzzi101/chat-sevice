package com.example.chatservice.message.controller;

import com.example.chatservice.message.controller.request.MessageRequest;
import com.example.chatservice.message.controller.response.MessageResponse;
import com.example.chatservice.message.service.MessageService;
import com.example.chatservice.user.CurrentUser;
import com.example.chatservice.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /*
     * 1. 메시지를 개인 or 단체를 보냄 (한 api 사용)
     * 2. 어차피 개인방도 챗룸을 가진다.
     */

    // TODO:FLOW - 5. 메시지를 /api/v1/messages/{chatRoomId}로 전송
    /*@PostMapping("/api/v1/messages/{chatRoomId}")
    public void sendMessage(
            @CurrentUser UserPrincipal userPrincipal,
            @RequestBody MessageRequest message,
            @PathVariable Long chatRoomId) {

        Long senderUserId = userPrincipal.getId();

        messageService.sendMessage(message, senderUserId, chatRoomId);
        // 여기서 응답받고 메시지 뿌리기 가능할 듯
        // relay
    }*/

    // TODO 3 : 메시지를 불러오기
    // 나에게 온 메시지를 들고오기 -> 이거는 따로 생각할 필요없이 한번에 다 불러와서 나눠주자

    /**
     * 프론트에서 좌, 우를 나눠서 표시해줌
     * 왼쪽은 상대방 메시지, 오른쪽은 내메시지로 한다면?
     * 메시지 조회했을 때 누가 보낸건지도 필요할 듯
     */
    // TODO readID? 확인하는 로직 넣기
    @GetMapping("/api/v1/messages/{chatRoomId}")
    public List<MessageResponse> getMessages(
            @CurrentUser UserPrincipal userPrincipal,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long lastReadMessageId,
            @RequestParam(defaultValue = "3") int before,
            @RequestParam(defaultValue = "3") int after
    ) {
        Long currentUserId = userPrincipal.getId();
        return messageService.getMessages(currentUserId, chatRoomId, lastReadMessageId, before, after);
    }

    @PostMapping("/api/v1/messages/{chatRoomId}/read")
    public void readMessage(
            @CurrentUser UserPrincipal userPrincipal,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) Long messageId
    ) {
        Long currentUserId = userPrincipal.getId();
        messageService.markMessagesAsRead(currentUserId, chatRoomId, messageId);
    }


}
