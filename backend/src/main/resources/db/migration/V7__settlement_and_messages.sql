-- Phase 6: RewardSettlementRequest and FamilyMessage tables
-- Settlement requests: child-initiated payout/spend awaiting parent approval

CREATE TABLE reward_settlement_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_user_id UUID NOT NULL,
    request_type VARCHAR(10) NOT NULL CHECK (request_type IN ('PAYOUT', 'SPEND')),
    requested_amount DOUBLE PRECISION NOT NULL CHECK (requested_amount > 0),
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    note TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by_parent_id UUID,
    reward_option_id UUID,
    CONSTRAINT fk_settlement_child FOREIGN KEY (child_user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_settlement_resolver FOREIGN KEY (resolved_by_parent_id)
        REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_settlement_reward_option FOREIGN KEY (reward_option_id)
        REFERENCES reward_options(id) ON DELETE SET NULL
);

CREATE INDEX idx_settlement_requests_child ON reward_settlement_requests(child_user_id);
CREATE INDEX idx_settlement_requests_status ON reward_settlement_requests(status);

-- Family messages: in-app nudge (child->parent) and encouragement (parent->child)

CREATE TABLE family_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_role VARCHAR(10) NOT NULL CHECK (sender_role IN ('PARENT', 'CHILD')),
    sender_user_id UUID NOT NULL,
    recipient_user_id UUID NOT NULL,
    message_type VARCHAR(15) NOT NULL CHECK (message_type IN ('NUDGE', 'ENCOURAGEMENT')),
    body VARCHAR(500) NOT NULL,
    linked_settlement_request_id UUID,
    email_notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_recipient FOREIGN KEY (recipient_user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_message_settlement FOREIGN KEY (linked_settlement_request_id)
        REFERENCES reward_settlement_requests(id) ON DELETE SET NULL
);

CREATE INDEX idx_family_messages_sender ON family_messages(sender_user_id);
CREATE INDEX idx_family_messages_recipient ON family_messages(recipient_user_id);
CREATE INDEX idx_family_messages_type ON family_messages(message_type, sender_user_id, created_at);
