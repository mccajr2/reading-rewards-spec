package com.example.readingrewards.domain.service;

import com.example.readingrewards.auth.service.VerificationEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional email via Brevo (Sendinblue) SMTP API.
 * Active when brevo.api.key is set to a non-empty value.
 */
@Service
@ConditionalOnProperty(name = "brevo.api.key", matchIfMissing = false)
public class BrevoEmailService implements VerificationEmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${spring.mail.from:noreply@reading-rewards.app}")
    private String mailFrom;

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private final RestTemplate restTemplate;

    public BrevoEmailService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean sendEmail(String to, String subject, String htmlContent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> payload = Map.of(
            "sender", Map.of("email", mailFrom),
            "to", List.of(Map.of("email", to)),
            "subject", subject,
            "htmlContent", htmlContent
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("[BrevoEmailService] Failed to send email to " + to + ": " + e.getMessage());
            return false;
        }
    }
}
