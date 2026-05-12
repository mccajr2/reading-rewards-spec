package com.example.readingrewards.domain.service.reward;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.domain.model.BookRead;
import com.example.readingrewards.domain.model.ChapterRead;
import com.example.readingrewards.domain.model.reward.RewardAccumulation;
import com.example.readingrewards.domain.model.reward.RewardSelection;
import com.example.readingrewards.domain.repo.ChapterReadRepository;
import com.example.readingrewards.domain.repo.reward.RewardAccumulationRepository;
import com.example.readingrewards.domain.repo.reward.RewardSelectionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class RewardAccumulationService {

    private final RewardSelectionRepository rewardSelectionRepository;
    private final RewardAccumulationRepository rewardAccumulationRepository;
    private final ChapterReadRepository chapterReadRepository;
    private final RewardCalculationService rewardCalculationService;

    public RewardAccumulationService(
        RewardSelectionRepository rewardSelectionRepository,
        RewardAccumulationRepository rewardAccumulationRepository,
        ChapterReadRepository chapterReadRepository,
        RewardCalculationService rewardCalculationService
    ) {
        this.rewardSelectionRepository = rewardSelectionRepository;
        this.rewardAccumulationRepository = rewardAccumulationRepository;
        this.chapterReadRepository = chapterReadRepository;
        this.rewardCalculationService = rewardCalculationService;
    }

    public RewardAccumulation recordBookCompletion(User child, BookRead bookRead) {
        if (child == null || bookRead == null) {
            return null;
        }

        UUID childId = child.getId();
        UUID bookReadId = bookRead.getId();
        if (childId == null || bookReadId == null) {
            return null;
        }

        List<RewardAccumulation> existing = rewardAccumulationRepository.findByBookReadIdOrderByCreatedAtDesc(bookReadId);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        RewardSelection selection = rewardSelectionRepository.findByBookReadIdAndActiveTrue(bookReadId).orElse(null);
        if (selection == null) {
            return null;
        }

        int unitCount = resolveUnitCount(selection, bookReadId);
        BigDecimal amount = rewardCalculationService.calculateAmount(selection.getLockedAmount(), unitCount);

        RewardAccumulation accumulation = new RewardAccumulation();
        accumulation.setChildId(childId);
        accumulation.setBookReadId(bookReadId);
        accumulation.setRewardTemplateId(selection.getRewardTemplateId());
        accumulation.setRewardType(com.example.readingrewards.domain.model.reward.RewardTemplate.RewardType.MONEY);
        accumulation.setUnitCount(unitCount);
        accumulation.setAmountEarned(amount);
        accumulation.setCalculationNote(
            rewardCalculationService.formatCalculationNote(
                com.example.readingrewards.domain.model.reward.RewardTemplate.RewardType.MONEY,
                selection.getLockedAmount(),
                unitCount
            )
        );
        accumulation.setStatus(RewardAccumulation.AccumulationStatus.EARNED);

        return rewardAccumulationRepository.save(accumulation);
    }

    private int resolveUnitCount(RewardSelection selection, UUID bookReadId) {
        if (selection.getLockedUnit() == com.example.readingrewards.domain.model.reward.RewardTemplate.RewardUnit.PER_BOOK) {
            return 1;
        }
        List<ChapterRead> chapterReads = chapterReadRepository.findByBookReadId(bookReadId);
        return Math.max(0, chapterReads.size());
    }
}
