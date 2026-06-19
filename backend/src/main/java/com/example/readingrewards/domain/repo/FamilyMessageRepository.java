package com.example.readingrewards.domain.repo;

import com.example.readingrewards.domain.model.FamilyMessage;
import com.example.readingrewards.domain.model.MessageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FamilyMessageRepository extends JpaRepository<FamilyMessage, UUID> {

    List<FamilyMessage> findBySenderUserIdOrRecipientUserIdOrderByCreatedAtDesc(
            UUID senderUserId, UUID recipientUserId);

    @Query("SELECT m FROM FamilyMessage m WHERE m.senderUserId = :senderId " +
           "AND m.messageType = :type ORDER BY m.createdAt DESC")
    List<FamilyMessage> findRecentBySenderAndType(
            @Param("senderId") UUID senderId,
            @Param("type") MessageType type);

    @Query("SELECT m FROM FamilyMessage m WHERE m.senderUserId = :senderId " +
           "AND m.messageType = :type AND m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<FamilyMessage> findBySenderAndTypeAfter(
            @Param("senderId") UUID senderId,
            @Param("type") MessageType type,
            @Param("since") LocalDateTime since);

    List<FamilyMessage> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);
}
