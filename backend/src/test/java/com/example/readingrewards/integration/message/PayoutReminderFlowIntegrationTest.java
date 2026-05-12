package com.example.readingrewards.integration.message;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.message.Message;
import com.example.readingrewards.domain.repo.message.MessageRepository;
import com.example.readingrewards.domain.service.message.PayoutReminderNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PayoutReminderFlowIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PayoutReminderNotificationService payoutReminderNotificationService;

    @BeforeEach
    void clean() {
        messageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void reminderMessageTransitionsFromUnreadToRead() {
        User parent = new User();
        parent.setEmail("parent-reminder@example.com");
        parent.setUsername("parent-reminder");
        parent.setPassword("pw");
        parent.setRole(User.UserRole.PARENT);
        parent = userRepository.save(parent);

        User child = new User();
        child.setEmail("child-reminder@example.com");
        child.setUsername("child-reminder");
        child.setPassword("pw");
        child.setRole(User.UserRole.CHILD);
        child.setParentId(parent.getId());
        child = userRepository.save(child);

        Message message = payoutReminderNotificationService.createReminder(
            child,
            parent,
            BigDecimal.valueOf(12.50),
            "Please check my payout",
            false
        );

        assertThat(message.isRead()).isFalse();
        assertThat(messageRepository.countByRecipientIdAndReadFalse(parent.getId())).isEqualTo(1);

        Message updated = payoutReminderNotificationService.markRead(message);

        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();
        assertThat(messageRepository.countByRecipientIdAndReadFalse(parent.getId())).isZero();
    }
}
