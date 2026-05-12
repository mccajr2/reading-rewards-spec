package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.domain.model.reward.RewardTemplate;
import org.springframework.stereotype.Service;

@Service
public class RewardSelectionPolicyService {

    public boolean canSelectTemplate(RewardTemplate template) {
        return template != null && !template.isDeleted();
    }
}
