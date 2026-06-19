package com.example.readingrewards.domain;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.*;
import com.example.readingrewards.domain.repo.RewardOptionRepository;
import com.example.readingrewards.domain.repo.RewardRepository;
import com.example.readingrewards.domain.repo.RewardSettlementRequestRepository;
import com.example.readingrewards.domain.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceUnitTests {

    @Mock
    private RewardSettlementRequestRepository settlementRepo;

    @Mock
    private RewardRepository rewardRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private RewardOptionRepository rewardOptionRepo;

    @InjectMocks
    private SettlementService settlementService;

    private User child;
    private User parent;

    @BeforeEach
    void setUp() {
        child = new User();
        child.setId(UUID.randomUUID());
        child.setRole(User.UserRole.CHILD);

        parent = new User();
        parent.setId(UUID.randomUUID());
        parent.setRole(User.UserRole.PARENT);
        child.setParentId(parent.getId());

        when(userRepo.findById(child.getId())).thenReturn(Optional.of(child));
        when(rewardRepo.findByUserId(child.getId())).thenReturn(List.of());
        when(settlementRepo.findByChildUserIdAndStatusOrderByRequestedAtDesc(
                child.getId(), SettlementRequestStatus.PENDING)).thenReturn(List.of());
    }

    @Test
    void createRequest_savesWithCorrectFields() {
        Reward earn = new Reward();
        earn.setType(RewardType.EARN);
        earn.setAmount(20.0);
        when(rewardRepo.findByUserId(child.getId())).thenReturn(List.of(earn));
        when(settlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RewardSettlementRequest result = settlementService.createRequest(
                child, SettlementRequestType.PAYOUT, 10.0, "Need cash", null);

        assertEquals(child.getId(), result.getChildUserId());
        assertEquals(SettlementRequestType.PAYOUT, result.getRequestType());
        assertEquals(10.0, result.getRequestedAmount());
        assertEquals(SettlementRequestStatus.PENDING, result.getStatus());
        assertEquals("Need cash", result.getNote());
    }

    @Test
    void createRequest_throwsBadRequest_whenAmountZero() {
        assertThrows(ResponseStatusException.class,
                () -> settlementService.createRequest(child, SettlementRequestType.PAYOUT, 0.0, null, null));
        verify(settlementRepo, never()).save(any());
    }

    @Test
    void createRequest_throwsBadRequest_whenAmountNegative() {
        assertThrows(ResponseStatusException.class,
                () -> settlementService.createRequest(child, SettlementRequestType.PAYOUT, -5.0, null, null));
    }

    @Test
    void approve_setsStatusAndCreatesLedgerEntry() {
        UUID requestId = UUID.randomUUID();
        RewardSettlementRequest pending = new RewardSettlementRequest();
        pending.setId(requestId);
        pending.setChildUserId(child.getId());
        pending.setRequestType(SettlementRequestType.PAYOUT);
        pending.setRequestedAmount(15.0);
        pending.setStatus(SettlementRequestStatus.PENDING);

        Reward earn = new Reward();
        earn.setType(RewardType.EARN);
        earn.setAmount(20.0);

        when(settlementRepo.findById(requestId)).thenReturn(Optional.of(pending));
        when(settlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rewardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rewardRepo.findByUserId(child.getId())).thenReturn(List.of(earn));

        settlementService.approve(requestId, parent);

        assertEquals(SettlementRequestStatus.APPROVED, pending.getStatus());
        assertEquals(parent.getId(), pending.getResolvedByParentId());
        assertNotNull(pending.getResolvedAt());

        ArgumentCaptor<Reward> ledgerCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepo).save(ledgerCaptor.capture());
        Reward ledger = ledgerCaptor.getValue();
        assertEquals(RewardType.PAYOUT, ledger.getType());
        assertEquals(15.0, ledger.getAmount());
        assertEquals(child.getId(), ledger.getUserId());
    }

    @Test
    void approve_spendRequest_createsSpendLedgerEntry() {
        UUID requestId = UUID.randomUUID();
        RewardSettlementRequest pending = new RewardSettlementRequest();
        pending.setId(requestId);
        pending.setChildUserId(child.getId());
        pending.setRequestType(SettlementRequestType.SPEND);
        pending.setRequestedAmount(5.0);
        pending.setStatus(SettlementRequestStatus.PENDING);

        Reward earn = new Reward();
        earn.setType(RewardType.EARN);
        earn.setAmount(10.0);

        when(settlementRepo.findById(requestId)).thenReturn(Optional.of(pending));
        when(settlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rewardRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rewardRepo.findByUserId(child.getId())).thenReturn(List.of(earn));

        settlementService.approve(requestId, parent);

        ArgumentCaptor<Reward> ledgerCaptor = ArgumentCaptor.forClass(Reward.class);
        verify(rewardRepo).save(ledgerCaptor.capture());
        assertEquals(RewardType.SPEND, ledgerCaptor.getValue().getType());
    }

    @Test
    void reject_setsStatusToRejected_noLedgerEntry() {
        UUID requestId = UUID.randomUUID();
        RewardSettlementRequest pending = new RewardSettlementRequest();
        pending.setId(requestId);
        pending.setStatus(SettlementRequestStatus.PENDING);

        when(settlementRepo.findById(requestId)).thenReturn(Optional.of(pending));
        when(settlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        settlementService.reject(requestId, parent);

        assertEquals(SettlementRequestStatus.REJECTED, pending.getStatus());
        verify(rewardRepo, never()).save(any());
    }

    @Test
    void cancel_setsStatusToCancelled() {
        UUID requestId = UUID.randomUUID();
        RewardSettlementRequest pending = new RewardSettlementRequest();
        pending.setId(requestId);
        pending.setChildUserId(child.getId());
        pending.setStatus(SettlementRequestStatus.PENDING);

        when(settlementRepo.findById(requestId)).thenReturn(Optional.of(pending));
        when(settlementRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        settlementService.cancel(requestId, child);

        assertEquals(SettlementRequestStatus.CANCELLED, pending.getStatus());
    }

    @Test
    void approve_throwsNotFound_whenRequestMissing() {
        UUID requestId = UUID.randomUUID();
        when(settlementRepo.findById(requestId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> settlementService.approve(requestId, parent));
    }

    @Test
    void approve_throwsConflict_whenAlreadyApproved() {
        UUID requestId = UUID.randomUUID();
        RewardSettlementRequest approved = new RewardSettlementRequest();
        approved.setId(requestId);
        approved.setStatus(SettlementRequestStatus.APPROVED);

        when(settlementRepo.findById(requestId)).thenReturn(Optional.of(approved));

        assertThrows(ResponseStatusException.class,
                () -> settlementService.approve(requestId, parent));
    }

    @Test
    void approve_throwsForbidden_whenParentDoesNotOwnChild() {
        UUID requestId = UUID.randomUUID();
        RewardSettlementRequest pending = new RewardSettlementRequest();
        pending.setId(requestId);
        pending.setChildUserId(child.getId());
        pending.setRequestedAmount(5.0);
        pending.setStatus(SettlementRequestStatus.PENDING);

        User otherParent = new User();
        otherParent.setId(UUID.randomUUID());
        otherParent.setRole(User.UserRole.PARENT);

        when(settlementRepo.findById(requestId)).thenReturn(Optional.of(pending));

        assertThrows(ResponseStatusException.class,
                () -> settlementService.approve(requestId, otherParent));
        verify(rewardRepo, never()).save(any());
    }

    @Test
    void createRequest_throwsBadRequest_whenInsufficientBalance() {
        Reward earn = new Reward();
        earn.setType(RewardType.EARN);
        earn.setAmount(2.0);
        when(rewardRepo.findByUserId(child.getId())).thenReturn(List.of(earn));

        assertThrows(ResponseStatusException.class,
                () -> settlementService.createRequest(child, SettlementRequestType.PAYOUT, 5.0, null, null));
        verify(settlementRepo, never()).save(any());
    }
}
