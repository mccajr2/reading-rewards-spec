package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.message.Message;
import com.example.readingrewards.domain.service.message.PayoutReminderNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/child/rewards/messages")
public class ChildMessageController {

    private final UserRepository userRepository;
    private final PayoutReminderNotificationService payoutReminderNotificationService;

    public ChildMessageController(
        UserRepository userRepository,
        PayoutReminderNotificationService payoutReminderNotificationService
    ) {
        this.userRepository = userRepository;
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
