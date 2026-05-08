package com.example.readingrewards.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(VerificationEmailService.class)
public class NoopVerificationEmailService implements VerificationEmailService {

    @Override
    public boolean sendEmail(String to, String subject, String htmlContent) {
        return true;
    }
}