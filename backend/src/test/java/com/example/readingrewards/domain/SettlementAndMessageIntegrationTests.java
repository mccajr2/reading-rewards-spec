package com.example.readingrewards.domain;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.auth.service.VerificationEmailService;
import com.example.readingrewards.domain.model.*;
import com.example.readingrewards.domain.repo.*;
import com.example.readingrewards.domain.service.MessageService;
import com.example.readingrewards.domain.service.SettlementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for settlement requests and family messaging (Phase 6).
 * Uses H2 in-memory DB via the test profile; MockMvc for HTTP layer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SettlementAndMessageIntegrationTests {

    @MockBean
    private VerificationEmailService verificationEmailService;

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepo;
    @Autowired private RewardRepository rewardRepo;
    @Autowired private RewardSettlementRequestRepository settlementRepo;
    @Autowired private FamilyMessageRepository messageRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private String parentJwt;
    private String childJwt;
    private UUID parentId;
    private UUID childId;

    @BeforeEach
    void setUp() throws Exception {
        messageRepo.deleteAll();
        settlementRepo.deleteAll();
        rewardRepo.deleteAll();
        userRepo.deleteAll();

        // parent
        User parent = new User();
        parent.setEmail("parent-settle@example.com");
        parent.setPassword(passwordEncoder.encode("pass123"));
        parent.setRole(User.UserRole.PARENT);
        parent.setFirstName("ParentUser");
        parent.setStatus("VERIFIED");
        parent = userRepo.save(parent);
        parentId = parent.getId();

        // child linked to parent
        User child = new User();
        child.setUsername("child-settle");
        child.setFirstName("ChildUser");
        child.setPassword(passwordEncoder.encode("kidpass"));
        child.setRole(User.UserRole.CHILD);
        child.setParentId(parentId);
        child.setStatus("VERIFIED");
        child = userRepo.save(child);
        childId = child.getId();

        parentJwt = login("parent-settle@example.com", "pass123");
        childJwt  = login("child-settle", "kidpass");

        // email service stub
        when(verificationEmailService.sendEmail(anyString(), anyString(), anyString()))
                .thenReturn(true);
    }

    // ─── Settlement: child creates request ──────────────────────────────────

    @Test
    void childCanCreatePayoutRequest() throws Exception {
        seedEarnedBalance(5.0);

        MvcResult result = mockMvc.perform(post("/api/children/" + childId + "/settlement-requests")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"PAYOUT","requestedAmount":5.00,"note":"I want my money"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.requestType").value("PAYOUT"))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("PENDING");
    }

    @Test
    void childCanCreateSpendRequest() throws Exception {
        seedEarnedBalance(5.0);

        mockMvc.perform(post("/api/children/" + childId + "/settlement-requests")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"SPEND","requestedAmount":2.50}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.requestType").value("SPEND"))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void childCannotCreateRequestForAnotherChild() throws Exception {
        // Create a second child
        User other = new User();
        other.setUsername("other-child");
        other.setPassword(passwordEncoder.encode("pass"));
        other.setRole(User.UserRole.CHILD);
        other.setParentId(parentId);
        other.setStatus("VERIFIED");
        other = userRepo.save(other);
        UUID otherId = other.getId();

        mockMvc.perform(post("/api/children/" + otherId + "/settlement-requests")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"PAYOUT","requestedAmount":1.00}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void negativeAmountIsRejected() throws Exception {
        mockMvc.perform(post("/api/children/" + childId + "/settlement-requests")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"PAYOUT","requestedAmount":-1.00}
                    """))
            .andExpect(status().isBadRequest());
    }

    // ─── Settlement: parent approve / reject ────────────────────────────────

    @Test
    void parentCanApproveRequest() throws Exception {
        RewardSettlementRequest req = createPendingRequest();

        mockMvc.perform(post("/api/settlement-requests/" + req.getId() + "/approve")
                .header("Authorization", "Bearer " + parentJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        // Should create a PAYOUT reward ledger entry
        List<Reward> rewards = rewardRepo.findAll();
        assertThat(rewards).hasSize(1);
        assertThat(rewards.get(0).getType()).isEqualTo(RewardType.PAYOUT);
        assertThat(rewards.get(0).getAmount()).isEqualTo(3.00);
    }

    @Test
    void parentCanRejectRequest() throws Exception {
        RewardSettlementRequest req = createPendingRequest();

        mockMvc.perform(post("/api/settlement-requests/" + req.getId() + "/reject")
                .header("Authorization", "Bearer " + parentJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        // Rejection should NOT create a ledger entry
        assertThat(rewardRepo.findAll()).isEmpty();
    }

    @Test
    void approvingAlreadyApprovedRequestFails() throws Exception {
        RewardSettlementRequest req = createPendingRequest();

        mockMvc.perform(post("/api/settlement-requests/" + req.getId() + "/approve")
                .header("Authorization", "Bearer " + parentJwt))
            .andExpect(status().isOk());

        // Second approve should conflict
        mockMvc.perform(post("/api/settlement-requests/" + req.getId() + "/approve")
                .header("Authorization", "Bearer " + parentJwt))
            .andExpect(status().isConflict());
    }

    @Test
    void parentCannotApproveUnrelatedChildRequest() throws Exception {
        seedEarnedBalance(5.0);
        RewardSettlementRequest req = createPendingRequest();

        User otherParent = new User();
        otherParent.setEmail("other-parent-settle@example.com");
        otherParent.setPassword(passwordEncoder.encode("pass123"));
        otherParent.setRole(User.UserRole.PARENT);
        otherParent.setStatus("VERIFIED");
        otherParent = userRepo.save(otherParent);
        String otherParentJwt = login("other-parent-settle@example.com", "pass123");

        mockMvc.perform(post("/api/settlement-requests/" + req.getId() + "/approve")
                .header("Authorization", "Bearer " + otherParentJwt))
            .andExpect(status().isForbidden());

        assertThat(rewardRepo.findAll()).isEmpty();
    }

    @Test
    void childCannotRequestMoreThanBalance() throws Exception {
        seedEarnedBalance(2.0);

        mockMvc.perform(post("/api/children/" + childId + "/settlement-requests")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"PAYOUT","requestedAmount":5.00}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void parentCanListAllPendingRequests() throws Exception {
        createPendingRequest();
        createPendingRequest();

        mockMvc.perform(get("/api/parent/settlement-requests")
                .header("Authorization", "Bearer " + parentJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void childCanCancelOwnPendingRequest() throws Exception {
        RewardSettlementRequest req = createPendingRequest();

        mockMvc.perform(delete("/api/settlement-requests/" + req.getId())
                .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ─── Messaging: nudge ───────────────────────────────────────────────────

    @Test
    void childCanSendNudgeToParent() throws Exception {
        mockMvc.perform(post("/api/messages/nudge")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"body":"Can you approve my payout?"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.messageType").value("NUDGE"))
            .andExpect(jsonPath("$.senderRole").value("CHILD"));
    }

    @Test
    void nudgeCooldownEnforcedWithin24Hours() throws Exception {
        // First nudge
        mockMvc.perform(post("/api/messages/nudge")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"First nudge\"}"))
            .andExpect(status().isCreated());

        // Second nudge within cooldown window
        mockMvc.perform(post("/api/messages/nudge")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Second nudge\"}"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void parentCannotSendNudge() throws Exception {
        mockMvc.perform(post("/api/messages/nudge")
                .header("Authorization", "Bearer " + parentJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"nudge from parent\"}"))
            .andExpect(status().isForbidden());
    }

    // ─── Messaging: encouragement ────────────────────────────────────────────

    @Test
    void parentCanSendEncouragementToChild() throws Exception {
        mockMvc.perform(post("/api/messages/encouragement/" + childId)
                .header("Authorization", "Bearer " + parentJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"body":"Great job reading this week!"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.messageType").value("ENCOURAGEMENT"))
            .andExpect(jsonPath("$.senderRole").value("PARENT"))
            .andExpect(jsonPath("$.emailNotificationSent").value(false));
    }

    @Test
    void childCannotSendEncouragement() throws Exception {
        mockMvc.perform(post("/api/messages/encouragement/" + childId)
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"from child\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void parentCannotSendEncouragementToUnrelatedChild() throws Exception {
        // Create another parent and their child
        User otherParent = new User();
        otherParent.setEmail("other-parent@example.com");
        otherParent.setPassword(passwordEncoder.encode("pass"));
        otherParent.setRole(User.UserRole.PARENT);
        otherParent.setStatus("VERIFIED");
        otherParent = userRepo.save(otherParent);

        User otherChild = new User();
        otherChild.setUsername("other-kid");
        otherChild.setPassword(passwordEncoder.encode("pass"));
        otherChild.setRole(User.UserRole.CHILD);
        otherChild.setParentId(otherParent.getId());
        otherChild.setStatus("VERIFIED");
        otherChild = userRepo.save(otherChild);

        // Our parent tries to send to the other parent's child
        mockMvc.perform(post("/api/messages/encouragement/" + otherChild.getId())
                .header("Authorization", "Bearer " + parentJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"not your child\"}"))
            .andExpect(status().isForbidden());
    }

    // ─── Messaging: inbox ────────────────────────────────────────────────────

    @Test
    void inboxReturnsMessagesAddressedToCurrentUser() throws Exception {
        // Send encouragement from parent to child — child's inbox should have it
        mockMvc.perform(post("/api/messages/encouragement/" + childId)
                .header("Authorization", "Bearer " + parentJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Keep it up!\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/messages/inbox")
                .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].messageType").value("ENCOURAGEMENT"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + identifier + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return body.get("token").toString();
    }

    private RewardSettlementRequest createPendingRequest() throws Exception {
        seedEarnedBalance(5.0);

        MvcResult result = mockMvc.perform(post("/api/children/" + childId + "/settlement-requests")
                .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"PAYOUT","requestedAmount":3.00,"note":"test"}
                    """))
            .andExpect(status().isCreated())
            .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return settlementRepo.findById(UUID.fromString(body.get("id").toString())).orElseThrow();
    }

    private void seedEarnedBalance(double amount) {
        Reward earn = new Reward();
        earn.setUserId(childId);
        earn.setType(RewardType.EARN);
        earn.setAmount(amount);
        rewardRepo.save(earn);
    }
}
