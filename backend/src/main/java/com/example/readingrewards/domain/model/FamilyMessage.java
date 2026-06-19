package com.example.readingrewards.domain.model;

import com.example.readingrewards.auth.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "family_messages")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class FamilyMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_role", nullable = false, length = 10)
    private String senderRole;

    @Column(name = "sender_user_id", nullable = false)
    private UUID senderUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User senderUser;

    @Column(name = "recipient_user_id", nullable = false)
    private UUID recipientUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 15)
    private MessageType messageType;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "linked_settlement_request_id")
    private UUID linkedSettlementRequestId;

    @Column(name = "email_notification_sent", nullable = false)
    private Boolean emailNotificationSent = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public UUID getSenderUserId() { return senderUserId; }
    public void setSenderUserId(UUID senderUserId) { this.senderUserId = senderUserId; }

    public User getSenderUser() { return senderUser; }

    public UUID getRecipientUserId() { return recipientUserId; }
    public void setRecipientUserId(UUID recipientUserId) { this.recipientUserId = recipientUserId; }

    public User getRecipientUser() { return recipientUser; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public UUID getLinkedSettlementRequestId() { return linkedSettlementRequestId; }
    public void setLinkedSettlementRequestId(UUID linkedSettlementRequestId) { this.linkedSettlementRequestId = linkedSettlementRequestId; }

    public Boolean getEmailNotificationSent() { return emailNotificationSent; }
    public void setEmailNotificationSent(Boolean emailNotificationSent) { this.emailNotificationSent = emailNotificationSent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
