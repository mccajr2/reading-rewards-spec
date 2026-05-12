package com.example.readingrewards.contract;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class ParentPerChildRewardContractTest {

    @Test
    @Disabled("US2 contract assertions will be enabled when per-child auth fixtures are finalized")
    void perChildRewardContractScaffold() {
        // Scaffold for POST /api/parent/rewards/child/{childId} and grouped retrieval checks.
    }
}
