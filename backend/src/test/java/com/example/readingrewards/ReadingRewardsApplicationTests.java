package com.example.readingrewards;

import com.example.readingrewards.auth.service.VerificationEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ReadingRewardsApplicationTests {

    @MockBean
    private VerificationEmailService verificationEmailService;

    @Test
    void contextLoads() {
    }
}