package com.example.readingrewards.contract;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class ChildRewardSelectionContractTest {

    @Test
    @Disabled("US4 contract assertions will be enabled when child auth fixtures are finalized")
    void childRewardSelectionContractScaffold() {
        // Scaffold for GET /api/child/rewards/available and POST /api/child/rewards/select/{bookReadId}.
    }
}
