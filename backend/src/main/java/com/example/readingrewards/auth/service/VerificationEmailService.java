package com.example.readingrewards.auth.service;

public interface VerificationEmailService {

    boolean sendEmail(String to, String subject, String htmlContent);
}