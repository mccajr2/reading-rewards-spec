package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.message.Message;
import com.example.readingrewards.domain.repo.message.MessageRepository;
import com.example.readingrewards.domain.service.message.PayoutReminderNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/child/rewards/messages")
public class ChildMessageController {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PayoutReminderNotificationService payoutReminderNotificationService;

    public ChildMessageController(
        UserRepository userRepository,
        MessageRepository messageRepository,
        PayoutReminderNotificationService payoutReminderNotificationService
    ) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.payoutReminderNotificationService = payoutReminderNotificationService;
    }

    @PostMapping("/payout-reminder")
    public ResponseEntity<?> sendPayoutReminder(
        @AuthenticationPrincipal UserDetails principal,
        @RequestBody PayoutReminderRequest request
    ) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }
        if (child.getParentId() == null) {
            return ResponseEntity.badRequest().body("Child account has no parent");
        }

        Optional<User> parentOpt = userRepository.findById(Objects.requireNonNull(child.getParentId()));
        if (parentOpt.isEmpty() || parentOpt.get().getRole() != User.UserRole.PARENT) {
            return ResponseEntity.badRequest().body("Parent account not found");
        }

        Message saved = payoutReminderNotificationService.createReminder(
            child,
            parentOpt.get(),
            request.pendingAmount(),
            request.note(),
            request.emailEnabled()
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", saved.getId());
        payload.put("recipientId", saved.getRecipientId());
        payload.put("messageType", saved.getMessageType());
        payload.put("messageText", saved.getMessageText());
        payload.put("createdAt", saved.getCreatedAt());
        return ResponseEntity.status(201).body(payload);
    }

    @GetMapping("/inbox")
    public ResponseEntity<?> listInbox(@AuthenticationPrincipal UserDetails principal) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        List<Map<String, Object>> messages = messageRepository
            .findByRecipientIdAndMessageTypeOrderByCreatedAtDesc(child.getId(), Message.MessageType.ENCOURAGEMENT)
            .stream()
            .map(this::toDto)
            .toList();

        long unreadCount = messages.stream()
            .filter(row -> Boolean.FALSE.equals(row.get("isRead")))
            .count();

        return ResponseEntity.ok(Map.of("messages", messages, "unreadCount", unreadCount));
    }

    @PostMapping("/inbox/{messageId}/read")
    public ResponseEntity<?> markInboxMessageRead(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID messageId
    ) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        return messageRepository.findByIdAndRecipientId(messageId, child.getId())
            .map(message -> {
                if (message.getMessageType() != Message.MessageType.ENCOURAGEMENT) {
                    return ResponseEntity.badRequest().body("Message is not an encouragement");
                }
                Message updated = payoutReminderNotificationService.markRead(message);
                return ResponseEntity.ok(toDto(updated));
            })
            .orElseGet(() -> ResponseEntity.status(404).body("Message not found"));
    }

    private Map<String, Object> toDto(Message message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messageId", message.getId());
        payload.put("senderId", message.getSenderId());
        payload.put("recipientId", message.getRecipientId());
        payload.put("messageType", message.getMessageType());
        payload.put("messageText", message.getMessageText());
        payload.put("isRead", message.isRead());
        payload.put("createdAt", message.getCreatedAt());
        payload.put("readAt", message.getReadAt());
        return payload;
    }

    private User resolveChild(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        String identifier = principal.getUsername();
        User child = identifier.contains("@")
            ? userRepository.findByEmail(identifier).orElse(null)
            : userRepository.findByUsername(identifier).orElse(null);
        if (child == null || child.getRole() != User.UserRole.CHILD) {
            return null;
        }
        return child;
    }

    public record PayoutReminderRequest(BigDecimal pendingAmount, String note, Boolean emailEnabled) {}
}
