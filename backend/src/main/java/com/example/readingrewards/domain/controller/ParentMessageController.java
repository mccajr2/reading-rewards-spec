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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/parent/rewards/messages")
public class ParentMessageController {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PayoutReminderNotificationService payoutReminderNotificationService;

    public ParentMessageController(
        UserRepository userRepository,
        MessageRepository messageRepository,
        PayoutReminderNotificationService payoutReminderNotificationService
    ) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.payoutReminderNotificationService = payoutReminderNotificationService;
    }

    @GetMapping("/payout-reminders")
    public ResponseEntity<?> listPayoutReminders(@AuthenticationPrincipal UserDetails principal) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        List<Map<String, Object>> reminders = messageRepository
            .findByRecipientIdAndMessageTypeOrderByCreatedAtDesc(parent.getId(), Message.MessageType.PAYOUT_REMINDER)
            .stream()
            .map(this::toDto)
            .toList();

        long unreadCount = reminders.stream()
            .filter(row -> Boolean.FALSE.equals(row.get("isRead")))
            .count();

        return ResponseEntity.ok(Map.of("reminders", reminders, "unreadCount", unreadCount));
    }

    @PostMapping("/payout-reminders/{messageId}/read")
    public ResponseEntity<?> markPayoutReminderRead(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID messageId
    ) {
        User parent = resolveParent(principal);
        if (parent == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        return messageRepository.findByIdAndRecipientId(messageId, parent.getId())
            .map(message -> {
                if (message.getMessageType() != Message.MessageType.PAYOUT_REMINDER) {
                    return ResponseEntity.badRequest().body("Message is not a payout reminder");
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

    private User resolveParent(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        String identifier = principal.getUsername();
        User parent = identifier.contains("@")
            ? userRepository.findByEmail(identifier).orElse(null)
            : userRepository.findByUsername(identifier).orElse(null);
        if (parent == null || parent.getRole() != User.UserRole.PARENT) {
            return null;
        }
        return parent;
    }
}
