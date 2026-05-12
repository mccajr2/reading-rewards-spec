package com.example.readingrewards.domain.service.message;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.service.VerificationEmailService;
import com.example.readingrewards.domain.model.message.Message;
import com.example.readingrewards.domain.repo.message.MessageRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class PayoutReminderNotificationService {

    private final MessageRepository messageRepository;
    private final VerificationEmailService verificationEmailService;

    public PayoutReminderNotificationService(
        MessageRepository messageRepository,
        ObjectProvider<VerificationEmailService> verificationEmailServiceProvider
    ) {
        this.messageRepository = messageRepository;
        this.verificationEmailService = verificationEmailServiceProvider.getIfAvailable(() -> (to, subject, htmlContent) -> true);
    }

    public Message createReminder(User child, User parent, BigDecimal pendingAmount, String note, Boolean emailEnabled) {
        Objects.requireNonNull(child, "child is required");
        Objects.requireNonNull(parent, "parent is required");

        Message message = new Message();
        message.setSenderId(child.getId());
        message.setRecipientId(parent.getId());
        message.setMessageType(Message.MessageType.PAYOUT_REMINDER);
        message.setMessageText(reminderText(child, pendingAmount, note));
        message.setRead(false);

        Message saved = messageRepository.save(message);
        if (isEmailEnabled(emailEnabled) && parent.getEmail() != null && !parent.getEmail().isBlank()) {
            verificationEmailService.sendEmail(
                parent.getEmail(),
                "Reading Rewards payout reminder",
                "<p>" + saved.getMessageText() + "</p>"
            );
        }
        return saved;
    }

    public Message markRead(Message message) {
        message.setRead(true);
        message.setReadAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public boolean isEmailEnabled(Boolean emailEnabled) {
        return emailEnabled == null || emailEnabled;
    }

    private String reminderText(User child, BigDecimal pendingAmount, String note) {
        String childName = child.getFirstName() != null && !child.getFirstName().isBlank()
            ? child.getFirstName()
            : child.getUsername();
        String amountText = pendingAmount == null ? "" : " ($" + pendingAmount + " pending)";
        String noteText = note == null || note.isBlank() ? "" : " - " + note.trim();
        return childName + " sent a payout reminder" + amountText + noteText;
    }
}
