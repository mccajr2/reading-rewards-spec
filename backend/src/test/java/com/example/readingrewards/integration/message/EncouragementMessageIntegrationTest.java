package com.example.readingrewards.integration.message;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.controller.ChildMessageController;
import com.example.readingrewards.domain.controller.ParentMessageController;
import com.example.readingrewards.domain.repo.message.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EncouragementMessageIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ParentMessageController parentMessageController;

    @Autowired
    private ChildMessageController childMessageController;

    @BeforeEach
    void clean() {
        messageRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void parentSendAndChildReadEncouragementMessage() {
        User parent = new User();
        parent.setEmail("parent-encouragement@example.com");
        parent.setUsername("parent-encouragement");
        parent.setPassword("pw");
        parent.setRole(User.UserRole.PARENT);
        parent = userRepository.save(parent);

        User child = new User();
        child.setEmail("child-encouragement@example.com");
        child.setUsername("child-encouragement");
        child.setPassword("pw");
        child.setRole(User.UserRole.CHILD);
        child.setParentId(parent.getId());
        child = userRepository.save(child);

        UserDetails parentPrincipal = org.springframework.security.core.userdetails.User
            .withUsername(parent.getEmail())
            .password("n/a")
            .authorities("ROLE_PARENT")
            .build();

        ResponseEntity<?> sendResponse = parentMessageController.sendEncouragement(
            parentPrincipal,
            new ParentMessageController.EncouragementRequest(child.getId(), "Great consistency this week!")
        );

        assertThat(sendResponse.getStatusCode().value()).isEqualTo(201);

        UserDetails childPrincipal = org.springframework.security.core.userdetails.User
            .withUsername(child.getEmail())
            .password("n/a")
            .authorities("ROLE_CHILD")
            .build();

        ResponseEntity<?> inboxResponse = childMessageController.listInbox(childPrincipal);
        assertThat(inboxResponse.getStatusCode().value()).isEqualTo(200);

        @SuppressWarnings("unchecked")
        Map<String, Object> inboxBody = (Map<String, Object>) Objects.requireNonNull(inboxResponse.getBody());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) inboxBody.get("messages");
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).get("messageText")).isEqualTo("Great consistency this week!");
        assertThat(messages.get(0).get("isRead")).isEqualTo(false);

        UUID messageId = UUID.fromString(String.valueOf(messages.get(0).get("messageId")));
        ResponseEntity<?> readResponse = childMessageController.markInboxMessageRead(childPrincipal, messageId);
        assertThat(readResponse.getStatusCode().value()).isEqualTo(200);

        assertThat(messageRepository.findById(Objects.requireNonNull(messageId)).orElseThrow().isRead()).isTrue();
    }
}
