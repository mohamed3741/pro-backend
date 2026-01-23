package com.sallahli.controller;

import com.sallahli.dto.chat.ConversationByParticipantRequest;
import com.sallahli.dto.chat.ConversationDto;
import com.sallahli.dto.chat.MessageDto;
import com.sallahli.model.Enum.AppEnum;
import com.sallahli.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/init-conversation")
    public ResponseEntity<ConversationDto> initConversation(@RequestBody ConversationDto conversationDto, @RequestParam(required = false) Long adminManagerId) {
        return ResponseEntity.ok(chatService.initConversation(conversationDto, adminManagerId));
    }

    @PostMapping("/send-message/conversation/{conversationId}")
    public ResponseEntity<MessageDto> sendMessage(@PathVariable Long conversationId, @RequestBody MessageDto messageDto) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, messageDto));
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<ConversationDto> getConversationById(@PathVariable Long conversationId) {
        return ResponseEntity.ok(chatService.getConversationById(conversationId));
    }

    @GetMapping("/conversation/{conversationId}/messages")
    public ResponseEntity<Page<MessageDto>> getMessages(@PathVariable Long conversationId, Pageable pageable) {
        return ResponseEntity.ok(chatService.getMessages(conversationId, pageable));
    }

    @PatchMapping("/conversation/{conversationId}/join")
    public ConversationDto joinConversation(@PathVariable Long conversationId) {
        return chatService.joinConversation(conversationId);
    }

    @PatchMapping("/conversation/{conversationId}/assign/{employeeId}")
    public ConversationDto assign(@PathVariable Long conversationId, @PathVariable Long employeeId) {
        return chatService.assign(conversationId, employeeId);
    }

    @PatchMapping("/conversation/{conversationId}/close")
    public ResponseEntity<Void> markConversationAsClosed(@PathVariable Long conversationId) {
        chatService.markConversationAsClosed(conversationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/by-participant")
    public Page<ConversationDto> findByParticipants(@RequestBody ConversationByParticipantRequest request, Pageable pageable) {
        return chatService.findByParticipantAndType(request, pageable);
    }

    @PatchMapping("/message/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(@PathVariable Long messageId) {
        chatService.markMessageAsRead(messageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/message/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadMessagesCount() {
        Map<String, Long> counts = chatService.getUnreadMessagesCount();
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/conversation/without-admin-participants")
    public ResponseEntity<Long> conversationsWithoutAdminParticipantsCount() {
        Long counts = chatService.conversationsWithoutAdminParticipantsCount();
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/conversations/unread-for-me")
    public ResponseEntity<Page<ConversationDto>> getUnreadConversationsForConnectedUser(
            @RequestParam AppEnum app,
            Pageable pageable) {
        Page<ConversationDto> conversations = chatService.getUnreadConversationsForConnectedUser(app, pageable);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/for-me")
    public ResponseEntity<Page<ConversationDto>> getConversationsForConnectedUser(
            @RequestParam AppEnum app,
            Pageable pageable) {
        Page<ConversationDto> conversations = chatService.getConversationsForConnectedUser(app, pageable);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/message/unread-count-for-me")
    public ResponseEntity<Long> getUnreadMessagesCountForConnectedUser(@RequestParam AppEnum app) {
        Long count = chatService.getUnreadMessagesCountForConnectedUser(app);
        return ResponseEntity.ok(count);
    }
}
