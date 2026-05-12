package com.example.readingrewards.integration.reward;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
public abstract class RewardApiIntegrationBaseTest {
    // Shared setup hooks and fixtures will be added as reward endpoints are implemented.
}
