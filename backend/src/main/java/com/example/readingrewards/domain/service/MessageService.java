package com.example.readingrewards.domain.service;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.auth.service.VerificationEmailService;
import com.example.readingrewards.domain.model.FamilyMessage;
import com.example.readingrewards.domain.model.MessageType;
import com.example.readingrewards.domain.repo.FamilyMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles in-app messaging between parents and children.
 *
 * Nudge rules (child → parent):
 *  - Only one nudge per child per rolling 24-hour window.
 *  - Sends email notification to parent.
 *
 * Encouragement rules (parent → child):
 *  - No cooldown.
 *  - In-app only, no email to child.
 */
@Service
public class MessageService {

    private static final int NUDGE_COOLDOWN_HOURS = 24;

    private final FamilyMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final VerificationEmailService emailService;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public MessageService(FamilyMessageRepository messageRepo,
                          UserRepository userRepo,
                          VerificationEmailService emailService) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    /**
     * Child sends a nudge to their parent. Enforces 24-hour cooldown and sends
     * parent an email notification.
     */
    @Transactional
    public FamilyMessage sendNudge(User child, String body, UUID linkedRequestId) {
        if (body == null || body.isBlank() || body.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nudge body must be 1–500 characters");
        }

        // Enforce cooldown
        LocalDateTime cooldownCutoff = LocalDateTime.now().minusHours(NUDGE_COOLDOWN_HOURS);
        List<FamilyMessage> recent = messageRepo.findBySenderAndTypeAfter(
                child.getId(), MessageType.NUDGE, cooldownCutoff);
        if (!recent.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Nudge cooldown active — one nudge per 24 hours");
        }

        User parent = resolveParent(child);

        FamilyMessage msg = new FamilyMessage();
        msg.setSenderRole("CHILD");
        msg.setSenderUserId(child.getId());
        msg.setRecipientUserId(parent.getId());
        msg.setMessageType(MessageType.NUDGE);
        msg.setBody(body.trim());
        msg.setLinkedSettlementRequestId(linkedRequestId);

        boolean sent = false;
        if (parent.getEmail() != null && !parent.getEmail().isBlank()) {
            String childName = child.getFirstName() != null ? child.getFirstName() : child.getUsername();
            String subject = childName + " sent you a nudge on Reading Rewards";
            String html = buildNudgeEmail(childName, body.trim());
            sent = emailService.sendEmail(parent.getEmail(), subject, html);
        }
        msg.setEmailNotificationSent(sent);

        return messageRepo.save(msg);
    }

    /**
     * Parent sends an encouragement message to a child. No cooldown, in-app only.
     */
    @Transactional
    public FamilyMessage sendEncouragement(User parent, UUID childId, String body,
                                           UUID linkedRequestId) {
        if (body == null || body.isBlank() || body.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Encouragement body must be 1–500 characters");
        }

        User child = userRepo.findById(childId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
        if (child.getRole() != User.UserRole.CHILD) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user is not a child");
        }
        if (!parent.getId().equals(child.getParentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Child does not belong to this parent");
        }

        FamilyMessage msg = new FamilyMessage();
        msg.setSenderRole("PARENT");
        msg.setSenderUserId(parent.getId());
        msg.setRecipientUserId(childId);
        msg.setMessageType(MessageType.ENCOURAGEMENT);
        msg.setBody(body.trim());
        msg.setLinkedSettlementRequestId(linkedRequestId);
        msg.setEmailNotificationSent(false);

        return messageRepo.save(msg);
    }

    public List<FamilyMessage> getMessagesForUser(UUID userId) {
        return messageRepo.findBySenderUserIdOrRecipientUserIdOrderByCreatedAtDesc(userId, userId);
    }

    public List<FamilyMessage> getInboxForUser(UUID userId) {
        return messageRepo.findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    private User resolveParent(User child) {
        if (child.getParentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Child has no associated parent");
        }
        return userRepo.findById(child.getParentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Parent not found"));
    }

    private String buildNudgeEmail(String childName, String body) {
        return """
                <html><body>
                <h2>Nudge from %s</h2>
                <p>%s</p>
                <p>Log in to <a href="%s">Reading Rewards</a> to respond.</p>
                </body></html>
                """.formatted(childName, body, frontendUrl);
    }
}
