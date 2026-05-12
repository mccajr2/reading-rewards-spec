package com.example.readingrewards.security;

import com.example.readingrewards.auth.model.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RewardAccessPolicy {

    public boolean canParentManageFamilyRewards(User principal) {
        return principal != null && principal.getRole() == User.UserRole.PARENT;
    }

    public boolean canParentManageChildRewards(User principal, UUID childParentId) {
        return principal != null
            && principal.getRole() == User.UserRole.PARENT
            && principal.getId() != null
            && principal.getId().equals(childParentId);
    }

    public boolean canChildAccessOwnRewards(User principal, UUID childId) {
        return principal != null
            && principal.getRole() == User.UserRole.CHILD
            && principal.getId() != null
            && principal.getId().equals(childId);
    }
}
