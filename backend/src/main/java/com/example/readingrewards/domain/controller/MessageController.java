package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.FamilyMessage;
import com.example.readingrewards.domain.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for family in-app messaging.
 *
 *   POST /api/messages/nudge                        — child sends nudge to parent
 *   POST /api/messages/encouragement/{childId}      — parent sends encouragement
 *   GET  /api/messages/inbox                        — messages addressed to caller
 *   GET  /api/messages                              — all messages involving caller
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepo;

    public MessageController(MessageService messageService, UserRepository userRepo) {
        this.messageService = messageService;
        this.userRepo = userRepo;
    }

    /** Child sends a nudge to their parent. Enforces 24-hour cooldown. */
    @PostMapping("/nudge")
    public ResponseEntity<FamilyMessage> sendNudge(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        User child = resolveChild(principal);
        String text = (String) body.get("body");
        UUID linkedId = body.get("linkedSettlementRequestId") != null
                ? UUID.fromString((String) body.get("linkedSettlementRequestId")) : null;

        FamilyMessage msg = messageService.sendNudge(child, text, linkedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    /** Parent sends an encouragement message to a specific child. */
    @PostMapping("/encouragement/{childId}")
    public ResponseEntity<FamilyMessage> sendEncouragement(
            @PathVariable UUID childId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        User parent = resolveParent(principal);
        String text = (String) body.get("body");
        UUID linkedId = body.get("linkedSettlementRequestId") != null
                ? UUID.fromString((String) body.get("linkedSettlementRequestId")) : null;

        FamilyMessage msg = messageService.sendEncouragement(parent, childId, text, linkedId);
        return ResponseEntity.status(HttpStatus.CREATED).body(msg);
    }

    /** Messages where the caller is the recipient (inbox). */
    @GetMapping("/inbox")
    public ResponseEntity<List<FamilyMessage>> inbox(
            @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(messageService.getInboxForUser(user.getId()));
    }

    /** All messages involving the caller (sent or received). */
    @GetMapping
    public ResponseEntity<List<FamilyMessage>> allMessages(
            @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(messageService.getMessagesForUser(user.getId()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User resolveUser(UserDetails principal) {
        String identifier = principal.getUsername();
        return identifier.contains("@")
                ? userRepo.findByEmail(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"))
                : userRepo.findByUsername(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User resolveChild(UserDetails principal) {
        User user = resolveUser(principal);
        if (user.getRole() != User.UserRole.CHILD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Child access required");
        }
        return user;
    }

    private User resolveParent(UserDetails principal) {
        User user = resolveUser(principal);
        if (user.getRole() != User.UserRole.PARENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent access required");
        }
        return user;
    }
}
