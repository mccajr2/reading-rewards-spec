package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.model.BookRead;
import com.example.readingrewards.domain.model.reward.ProgressTracking;
import com.example.readingrewards.domain.model.reward.RewardSelection;
import com.example.readingrewards.domain.model.reward.RewardTemplate;
import com.example.readingrewards.domain.repo.BookReadRepository;
import com.example.readingrewards.domain.repo.reward.ProgressTrackingRepository;
import com.example.readingrewards.domain.repo.reward.RewardSelectionRepository;
import com.example.readingrewards.domain.repo.reward.RewardTemplateRepository;
import com.example.readingrewards.domain.service.reward.ProgressTrackingPolicyService;
import com.example.readingrewards.external.OpenLibraryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/child/rewards/progress")
public class ChildProgressTrackingController {

    private final UserRepository userRepository;
    private final BookReadRepository bookReadRepository;
    private final RewardSelectionRepository rewardSelectionRepository;
    private final RewardTemplateRepository rewardTemplateRepository;
    private final ProgressTrackingRepository progressTrackingRepository;
    private final ProgressTrackingPolicyService progressTrackingPolicyService;
    private final OpenLibraryClient openLibraryClient;

    public ChildProgressTrackingController(
        UserRepository userRepository,
        BookReadRepository bookReadRepository,
        RewardSelectionRepository rewardSelectionRepository,
        RewardTemplateRepository rewardTemplateRepository,
        ProgressTrackingRepository progressTrackingRepository,
        ProgressTrackingPolicyService progressTrackingPolicyService,
        OpenLibraryClient openLibraryClient
    ) {
        this.userRepository = userRepository;
        this.bookReadRepository = bookReadRepository;
        this.rewardSelectionRepository = rewardSelectionRepository;
        this.rewardTemplateRepository = rewardTemplateRepository;
        this.progressTrackingRepository = progressTrackingRepository;
        this.progressTrackingPolicyService = progressTrackingPolicyService;
        this.openLibraryClient = openLibraryClient;
    }

    @GetMapping("/{bookReadId}")
    public ResponseEntity<?> getTracking(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID bookReadId
    ) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        UUID safeBookReadId = Objects.requireNonNull(bookReadId);
        Optional<BookRead> opt = bookReadRepository.findById(safeBookReadId);
        if (opt.isEmpty() || !child.getId().equals(opt.get().getUserId())) {
            return ResponseEntity.status(404).body("Book read not found");
        }

        BookRead bookRead = opt.get();
        ProgressTracking tracking = progressTrackingRepository.findByBookReadId(safeBookReadId).orElseGet(() -> {
            ProgressTracking created = new ProgressTracking();
            created.setBookReadId(safeBookReadId);
            created.setTrackingType(ProgressTracking.TrackingType.NONE);
            return created;
        });

        OptionalInt suggested = openLibraryClient.fetchPageCountByBookId(bookRead.getGoogleBookId());
        RewardTemplate.RewardUnit rewardUnit = resolveRewardUnit(safeBookReadId).orElse(RewardTemplate.RewardUnit.PER_BOOK);

        return ResponseEntity.ok(toResponse(safeBookReadId, tracking, suggested.isPresent() ? suggested.getAsInt() : null, rewardUnit));
    }

    @PutMapping("/{bookReadId}")
    public ResponseEntity<?> updateTracking(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID bookReadId,
        @RequestBody ProgressUpdateRequest request
    ) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        UUID safeBookReadId = Objects.requireNonNull(bookReadId);
        Optional<BookRead> opt = bookReadRepository.findById(safeBookReadId);
        if (opt.isEmpty() || !child.getId().equals(opt.get().getUserId())) {
            return ResponseEntity.status(404).body("Book read not found");
        }

        BookRead bookRead = opt.get();
        ProgressTracking tracking = progressTrackingRepository.findByBookReadId(safeBookReadId).orElseGet(() -> {
            ProgressTracking created = new ProgressTracking();
            created.setBookReadId(safeBookReadId);
            return created;
        });

        ProgressTracking.TrackingType trackingType = request.trackingType() == null
            ? ProgressTracking.TrackingType.NONE
            : request.trackingType();

        tracking.setTrackingType(trackingType);
        tracking.setTotalChapters(request.totalChapters());
        tracking.setCurrentChapter(request.currentChapter());

        Integer resolvedTotalPages = openLibraryClient.suggestedPageCount(bookRead.getGoogleBookId(), request.totalPages())
            .stream()
            .boxed()
            .findFirst()
            .orElse(null);

        tracking.setTotalPages(resolvedTotalPages);
        tracking.setCurrentPage(request.currentPage());

        try {
            progressTrackingPolicyService.validateTrackingConsistency(tracking);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

        ProgressTracking saved = progressTrackingRepository.save(tracking);
        RewardTemplate.RewardUnit rewardUnit = resolveRewardUnit(safeBookReadId).orElse(RewardTemplate.RewardUnit.PER_BOOK);
        return ResponseEntity.ok(toResponse(safeBookReadId, saved, resolvedTotalPages, rewardUnit));
    }

    @PostMapping("/{bookReadId}/validate-complete")
    public ResponseEntity<?> validateCompletion(
        @AuthenticationPrincipal UserDetails principal,
        @PathVariable UUID bookReadId
    ) {
        User child = resolveChild(principal);
        if (child == null) {
            return ResponseEntity.status(403).body("Not authorized");
        }

        UUID safeBookReadId = Objects.requireNonNull(bookReadId);
        Optional<BookRead> opt = bookReadRepository.findById(safeBookReadId);
        if (opt.isEmpty() || !child.getId().equals(opt.get().getUserId())) {
            return ResponseEntity.status(404).body("Book read not found");
        }

        RewardTemplate.RewardUnit rewardUnit = resolveRewardUnit(safeBookReadId).orElse(RewardTemplate.RewardUnit.PER_BOOK);
        ProgressTracking tracking = progressTrackingRepository.findByBookReadId(safeBookReadId).orElse(null);

        try {
            progressTrackingPolicyService.validateCompletionAllowed(rewardUnit, tracking);
            return ResponseEntity.ok(Map.of("allowed", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("allowed", false, "message", ex.getMessage()));
        }
    }

    private Map<String, Object> toResponse(UUID bookReadId, ProgressTracking tracking, Integer suggestedPageCount, RewardTemplate.RewardUnit rewardUnit) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookReadId", bookReadId);
        payload.put("trackingType", tracking.getTrackingType());
        payload.put("totalChapters", tracking.getTotalChapters());
        payload.put("currentChapter", tracking.getCurrentChapter());
        payload.put("totalPages", tracking.getTotalPages());
        payload.put("currentPage", tracking.getCurrentPage());
        payload.put("suggestedPageCount", suggestedPageCount);
        payload.put("rewardUnit", rewardUnit);
        payload.put("completionAllowed", progressTrackingPolicyService.canComplete(rewardUnit, tracking));
        return payload;
    }

    private Optional<RewardTemplate.RewardUnit> resolveRewardUnit(UUID bookReadId) {
        return rewardSelectionRepository.findByBookReadIdAndActiveTrue(bookReadId)
            .map(RewardSelection::getRewardTemplateId)
            .flatMap(rewardTemplateRepository::findById)
            .map(RewardTemplate::getUnit);
    }

    private User resolveChild(UserDetails principal) {
        if (principal == null) {
            return null;
        }
        String identifier = principal.getUsername();
        User child = identifier.contains("@")
            ? userRepository.findByEmail(identifier).orElse(null)
            : userRepository.findByUsername(identifier).orElse(null);
        if (child == null || child.getRole() != User.UserRole.CHILD) {
            return null;
        }
        return child;
    }

    public record ProgressUpdateRequest(
        ProgressTracking.TrackingType trackingType,
        Integer totalChapters,
        Integer currentChapter,
        Integer totalPages,
        Integer currentPage
    ) {}
}
