package com.example.readingrewards.security;

import com.example.readingrewards.auth.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RewardRoleAuthorizationTest {

    private final RewardAccessPolicy policy = new RewardAccessPolicy();

    @Test
    void parentCannotAccessChildOnlyRewardEndpoints() {
        User parent = new User();
        parent.setId(UUID.randomUUID());
        parent.setRole(User.UserRole.PARENT);

        boolean allowed = policy.canChildAccessOwnRewards(parent, parent.getId());
        assertThat(allowed).isFalse();
    }

    @Test
    void childCanOnlyAccessOwnRewards() {
        User child = new User();
        child.setId(UUID.randomUUID());
        child.setRole(User.UserRole.CHILD);

        boolean ownAllowed = policy.canChildAccessOwnRewards(child, child.getId());
        boolean otherAllowed = policy.canChildAccessOwnRewards(child, UUID.randomUUID());

        assertThat(ownAllowed).isTrue();
        assertThat(otherAllowed).isFalse();
    }
}
