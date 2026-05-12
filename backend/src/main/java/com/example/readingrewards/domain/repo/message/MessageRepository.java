package com.example.readingrewards.domain.repo.message;

import com.example.readingrewards.domain.model.message.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
    List<Message> findByRecipientIdAndMessageTypeOrderByCreatedAtDesc(UUID recipientId, Message.MessageType messageType);
    Optional<Message> findByIdAndRecipientId(UUID id, UUID recipientId);
    long countByRecipientIdAndReadFalse(UUID recipientId);
}
