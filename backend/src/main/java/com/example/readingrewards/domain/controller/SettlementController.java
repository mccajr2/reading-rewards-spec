package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.RewardSettlementRequest;
import com.example.readingrewards.domain.model.SettlementRequestType;
import com.example.readingrewards.domain.service.SettlementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for reward settlement requests.
 *
 * Child endpoints:
 *   POST   /api/children/{childId}/settlement-requests       — create request
 *   GET    /api/children/{childId}/settlement-requests       — list requests
 *   DELETE /api/settlement-requests/{requestId}              — cancel (child)
 *
 * Parent endpoints:
 *   GET    /api/parent/settlement-requests                   — list all children's pending
 *   POST   /api/settlement-requests/{requestId}/approve      — approve
 *   POST   /api/settlement-requests/{requestId}/reject       — reject
 */
@RestController
@RequestMapping("/api")
public class SettlementController {

    private final SettlementService settlementService;
    private final UserRepository userRepo;

    public SettlementController(SettlementService settlementService,
                                UserRepository userRepo) {
        this.settlementService = settlementService;
        this.userRepo = userRepo;
    }

    // ── Child: create settlement request ────────────────────────────────────

    @PostMapping("/children/{childId}/settlement-requests")
    public ResponseEntity<RewardSettlementRequest> createRequest(
            @PathVariable UUID childId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = resolveUser(principal);

        // Allow child acting on themselves, or parent acting for their child
        if (caller.getRole() == User.UserRole.CHILD) {
            if (!caller.getId().equals(childId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot create for another child");
            }
        } else if (caller.getRole() == User.UserRole.PARENT) {
            User child = requireChild(childId);
            if (!caller.getId().equals(child.getParentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Child not in your family");
            }
        }

        String typeStr = (String) body.get("requestType");
        if (typeStr == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestType required");
        SettlementRequestType type;
        try { type = SettlementRequestType.valueOf(typeStr); }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid requestType");
        }

        double amount = parseDouble(body.get("requestedAmount"));
        String note = (String) body.get("note");
        UUID rewardOptionId = body.get("rewardOptionId") != null
                ? UUID.fromString((String) body.get("rewardOptionId")) : null;

        User child = resolveChildForRequest(caller, childId);
        RewardSettlementRequest req = settlementService.createRequest(child, type, amount, note, rewardOptionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    @GetMapping("/children/{childId}/settlement-requests")
    public ResponseEntity<List<RewardSettlementRequest>> listRequests(
            @PathVariable UUID childId,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = resolveUser(principal);
        authorizeChildAccess(caller, childId);
        return ResponseEntity.ok(settlementService.getRequestsForChild(childId));
    }

    @DeleteMapping("/settlement-requests/{requestId}")
    public ResponseEntity<RewardSettlementRequest> cancelRequest(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetails principal) {

        User caller = resolveUser(principal);
        if (caller.getRole() != User.UserRole.CHILD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only child can cancel");
        }
        return ResponseEntity.ok(settlementService.cancel(requestId, caller));
    }

    // ── Parent: list pending settlement requests for all children ───────────

    @GetMapping("/parent/settlement-requests")
    public ResponseEntity<List<RewardSettlementRequest>> listPendingForParent(
            @AuthenticationPrincipal UserDetails principal) {

        User parent = resolveParent(principal);
        List<User> children = userRepo.findByParentId(parent.getId());
        List<RewardSettlementRequest> all = children.stream()
                .flatMap(child -> settlementService.getPendingRequestsForChild(child.getId()).stream())
                .toList();
        return ResponseEntity.ok(all);
    }

    // ── Parent: approve / reject ─────────────────────────────────────────────

    @PostMapping("/settlement-requests/{requestId}/approve")
    public ResponseEntity<RewardSettlementRequest> approve(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetails principal) {

        User parent = resolveParent(principal);
        return ResponseEntity.ok(settlementService.approve(requestId, parent));
    }

    @PostMapping("/settlement-requests/{requestId}/reject")
    public ResponseEntity<RewardSettlementRequest> reject(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal UserDetails principal) {

        User parent = resolveParent(principal);
        return ResponseEntity.ok(settlementService.reject(requestId, parent));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User resolveUser(UserDetails principal) {
        String identifier = principal.getUsername();
        return identifier.contains("@")
                ? userRepo.findByEmail(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"))
                : userRepo.findByUsername(identifier).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private User resolveParent(UserDetails principal) {
        User user = resolveUser(principal);
        if (user.getRole() != User.UserRole.PARENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Parent access required");
        }
        return user;
    }

    private User requireChild(UUID childId) {
        return userRepo.findById(childId)
                .filter(u -> u.getRole() == User.UserRole.CHILD)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Child not found"));
    }

    private User resolveChildForRequest(User caller, UUID childId) {
        if (caller.getRole() == User.UserRole.CHILD) return caller;
        return requireChild(childId);
    }

    private void authorizeChildAccess(User caller, UUID childId) {
        if (caller.getRole() == User.UserRole.CHILD) {
            if (!caller.getId().equals(childId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot access another child's data");
            }
        } else if (caller.getRole() == User.UserRole.PARENT) {
            User child = requireChild(childId);
            if (!caller.getId().equals(child.getParentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Child not in your family");
            }
        }
    }

    private double parseDouble(Object val) {
        if (val == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestedAmount required");
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestedAmount must be a number");
        }
    }
}
