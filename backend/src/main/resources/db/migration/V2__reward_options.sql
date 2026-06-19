CREATE TABLE reward_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    child_user_id UUID,
    scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('FAMILY', 'CHILD')),
    name VARCHAR(120) NOT NULL,
    description TEXT,
    earning_basis VARCHAR(30) NOT NULL CHECK (earning_basis IN ('PER_CHAPTER', 'PER_BOOK', 'PER_PAGE_MILESTONE')),
    amount DOUBLE PRECISION NOT NULL,
    page_milestone_size INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reward_option_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reward_option_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT reward_option_child_scope CHECK (
        (scope_type = 'FAMILY' AND child_user_id IS NULL)
        OR (scope_type = 'CHILD' AND child_user_id IS NOT NULL)
    ),
    CONSTRAINT reward_option_amount_positive CHECK (amount > 0),
    CONSTRAINT reward_option_page_milestone_rule CHECK (
        (earning_basis = 'PER_PAGE_MILESTONE' AND page_milestone_size IS NOT NULL AND page_milestone_size > 0)
        OR (earning_basis <> 'PER_PAGE_MILESTONE' AND page_milestone_size IS NULL)
    )
);

CREATE INDEX idx_reward_options_owner_user_id ON reward_options(owner_user_id);
CREATE INDEX idx_reward_options_child_user_id ON reward_options(child_user_id);

CREATE TABLE child_reward_selections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_user_id UUID NOT NULL,
    reward_option_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    selected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_child_reward_selection_child FOREIGN KEY (child_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_child_reward_selection_option FOREIGN KEY (reward_option_id) REFERENCES reward_options(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_child_reward_selection_active ON child_reward_selections(child_user_id) WHERE active = TRUE;
CREATE INDEX idx_child_reward_selection_option_id ON child_reward_selections(reward_option_id);

ALTER TABLE rewards ADD COLUMN reward_option_id UUID;
ALTER TABLE rewards ADD CONSTRAINT fk_reward_option FOREIGN KEY (reward_option_id) REFERENCES reward_options(id) ON DELETE SET NULL;
CREATE INDEX idx_rewards_reward_option_id ON rewards(reward_option_id);