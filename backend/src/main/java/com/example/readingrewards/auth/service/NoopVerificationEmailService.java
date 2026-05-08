package com.example.readingrewards.auth.service;

import org.springframework.stereotype.Service;

@Service
public class NoopVerificationEmailService implements VerificationEmailService {

    @Override
    public boolean sendEmail(String to, String subject, String htmlContent) {
        return true;
    }
}