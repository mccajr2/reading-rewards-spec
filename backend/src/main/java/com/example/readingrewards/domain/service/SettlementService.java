package com.example.readingrewards.domain.service;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.*;
import com.example.readingrewards.domain.repo.RewardOptionRepository;
import com.example.readingrewards.domain.repo.RewardRepository;
import com.example.readingrewards.domain.repo.RewardSettlementRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles child-initiated settlement requests (payout/spend) and parent
 * approval/rejection. On approval, creates the corresponding Reward ledger entry.
 */
@Service
public class SettlementService {

    private final RewardSettlementRequestRepository settlementRepo;
    private final RewardRepository rewardRepo;
    private final UserRepository userRepo;
    private final RewardOptionRepository rewardOptionRepo;

    public SettlementService(RewardSettlementRequestRepository settlementRepo,
                             RewardRepository rewardRepo,
                             UserRepository userRepo,
                             RewardOptionRepository rewardOptionRepo) {
        this.settlementRepo = settlementRepo;
        this.rewardRepo = rewardRepo;
        this.userRepo = userRepo;
        this.rewardOptionRepo = rewardOptionRepo;
    }

    @Transactional
    public RewardSettlementRequest createRequest(User child, SettlementRequestType type,
                                                  double amount, String note,
                                                  UUID rewardOptionId) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        validateRewardOption(child, rewardOptionId);
        ensureSufficientBalance(child.getId(), rewardOptionId, amount, null);

        RewardSettlementRequest req = new RewardSettlementRequest();
        req.setChildUserId(child.getId());
        req.setRequestType(type);
        req.setRequestedAmount(amount);
        req.setNote(note);
        req.setRewardOptionId(rewardOptionId);
        return settlementRepo.save(req);
    }

    public List<RewardSettlementRequest> getRequestsForChild(UUID childId) {
        return settlementRepo.findByChildUserIdOrderByRequestedAtDesc(childId);
    }

    public List<RewardSettlementRequest> getPendingRequestsForChild(UUID childId) {
        return settlementRepo.findByChildUserIdAndStatusOrderByRequestedAtDesc(
                childId, SettlementRequestStatus.PENDING);
    }

    @Transactional
    public RewardSettlementRequest approve(UUID requestId, User parent) {
        RewardSettlementRequest req = findPending(requestId);
        User child = requireChild(req.getChildUserId());
        authorizeParentForChild(parent, child);
        ensureSufficientBalance(child.getId(), req.getRewardOptionId(),
                req.getRequestedAmount(), req.getId());

        req.setStatus(SettlementRequestStatus.APPROVED);
        req.setResolvedAt(LocalDateTime.now());
        req.setResolvedByParentId(parent.getId());
        settlementRepo.save(req);

        Reward ledgerEntry = new Reward();
        ledgerEntry.setUserId(req.getChildUserId());
        ledgerEntry.setType(req.getRequestType() == SettlementRequestType.PAYOUT
                ? RewardType.PAYOUT : RewardType.SPEND);
        ledgerEntry.setAmount(req.getRequestedAmount());
        ledgerEntry.setNote(req.getNote() != null ? req.getNote()
                : req.getRequestType().name().toLowerCase() + " approved");
        if (req.getRewardOptionId() != null) {
            ledgerEntry.setRewardOptionId(req.getRewardOptionId());
        }
        rewardRepo.save(ledgerEntry);

        return req;
    }

    @Transactional
    public RewardSettlementRequest reject(UUID requestId, User parent) {
        RewardSettlementRequest req = findPending(requestId);
        User child = requireChild(req.getChildUserId());
        authorizeParentForChild(parent, child);

        req.setStatus(SettlementRequestStatus.REJECTED);
        req.setResolvedAt(LocalDateTime.now());
        req.setResolvedByParentId(parent.getId());
        return settlementRepo.save(req);
    }

    @Transactional
    public RewardSettlementRequest cancel(UUID requestId, User child) {
        RewardSettlementRequest req = findPending(requestId);
        if (!req.getChildUserId().equals(child.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your request");
        }
        req.setStatus(SettlementRequestStatus.CANCELLED);
        return settlementRepo.save(req);
    }

    private RewardSettlementRequest findPending(UUID requestId) {
        RewardSettlementRequest req = settlementRepo.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Settlement request not found"));
        if (req.getStatus() != SettlementRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Request is not in PENDING state");
        }
        return req;
    }

    private User requireChild(UUID childId) {
        return userRepo.findById(childId)
                .filter(u -> u.getRole() == User.UserRole.CHILD)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
    }

    private void authorizeParentForChild(User parent, User child) {
        if (parent.getRole() != User.UserRole.PARENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent access required");
        }
        if (child.getParentId() == null || !child.getParentId().equals(parent.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Child not in your family");
        }
    }

    private void validateRewardOption(User child, UUID rewardOptionId) {
        if (rewardOptionId == null) {
            return;
        }
        RewardOption option = rewardOptionRepo.findById(rewardOptionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Reward option not found"));
        if (option.getChildUserId() != null && !option.getChildUserId().equals(child.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Reward option is not available to this child");
        }
    }

    private void ensureSufficientBalance(UUID childId, UUID rewardOptionId, double amount,
                                         UUID excludeRequestId) {
        double available = getAvailableBalance(childId, rewardOptionId, excludeRequestId);
        if (amount > available) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient balance for this request");
        }
    }

    private double getAvailableBalance(UUID childId, UUID rewardOptionId, UUID excludeRequestId) {
        String unitKey = resolveUnitKey(rewardOptionId);
        double earned = 0;
        double paidOut = 0;
        double spent = 0;

        for (Reward reward : rewardRepo.findByUserId(childId)) {
            if (!matchesUnit(reward, unitKey, rewardOptionId)) {
                continue;
            }
            if (reward.getType() == RewardType.EARN) {
                earned += reward.getAmount();
            } else if (reward.getType() == RewardType.PAYOUT) {
                paidOut += reward.getAmount();
            } else if (reward.getType() == RewardType.SPEND) {
                spent += reward.getAmount();
            }
        }

        double pending = settlementRepo
                .findByChildUserIdAndStatusOrderByRequestedAtDesc(childId, SettlementRequestStatus.PENDING)
                .stream()
                .filter(req -> excludeRequestId == null || !excludeRequestId.equals(req.getId()))
                .filter(req -> matchesRequestUnit(req, unitKey, rewardOptionId))
                .mapToDouble(RewardSettlementRequest::getRequestedAmount)
                .sum();

        return earned - paidOut - spent - pending;
    }

    private boolean matchesUnit(Reward reward, String unitKey, UUID rewardOptionId) {
        if (rewardOptionId != null) {
            return unitKey.equals(resolveUnitKey(reward.getRewardOptionId()));
        }
        return true;
    }

    private boolean matchesRequestUnit(RewardSettlementRequest request, String unitKey,
                                       UUID rewardOptionId) {
        if (rewardOptionId != null) {
            return unitKey.equals(resolveUnitKey(request.getRewardOptionId()));
        }
        return request.getRewardOptionId() == null;
    }

    private String resolveUnitKey(UUID rewardOptionId) {
        if (rewardOptionId == null) {
            return "ALL";
        }
        RewardOption option = rewardOptionRepo.findById(rewardOptionId).orElse(null);
        if (option == null || option.getValueType() == null
                || option.getValueType() == RewardValueType.MONEY) {
            String currency = option != null && option.getCurrencyCode() != null
                    && !option.getCurrencyCode().isBlank()
                    ? option.getCurrencyCode().toUpperCase(Locale.ROOT)
                    : "USD";
            return "MONEY|" + currency;
        }
        String label = option.getNonMoneyUnitLabel() != null && !option.getNonMoneyUnitLabel().isBlank()
                ? option.getNonMoneyUnitLabel().trim()
                : "units";
        return "NON_MONEY|" + label.toLowerCase(Locale.ROOT);
    }
}
