package com.example.readingrewards.domain.service;

import com.example.readingrewards.domain.model.*;
import com.example.readingrewards.domain.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing reading progress tracking with support for
 * different tracking modes (BOOK_ONLY, CHAPTERS, PAGES) and earning bases.
 */
@Service
public class ReadingProgressService {

    private final BookReadRepository bookReadRepository;
    private final RewardRepository rewardRepository;
    private final RewardOptionRepository rewardOptionRepository;
    private final ChildRewardSelectionRepository rewardSelectionRepository;

    public ReadingProgressService(BookReadRepository bookReadRepository,
                                 RewardRepository rewardRepository,
                                 RewardOptionRepository rewardOptionRepository,
                                 ChildRewardSelectionRepository rewardSelectionRepository) {
        this.bookReadRepository = bookReadRepository;
        this.rewardRepository = rewardRepository;
        this.rewardOptionRepository = rewardOptionRepository;
        this.rewardSelectionRepository = rewardSelectionRepository;
    }

    /**
     * Select and lock the earning basis for a book assignment.
     * This is called when a child starts reading a book with a specific tracking mode.
     */
    @Transactional
    public BookRead selectBasisForBook(UUID childId, String googleBookId, 
                                       RewardEarningBasis earningBasis,
                                       Integer suggestedPageCount,
                                       Integer totalPagesOverride,
                                       Boolean pageCountConfirmed) {
        // Find the active book read for this child
        Optional<BookRead> existingRead = bookReadRepository.findByUserIdAndGoogleBookIdAndEndDateIsNull(childId, googleBookId);
        
        if (existingRead.isPresent()) {
            BookRead bookRead = existingRead.get();
            
            // Check if basis is already locked
            if (bookRead.getBasisLockedAt() != null) {
                throw new IllegalStateException("Basis already locked for this book");
            }
            
            // Set earning basis and lock it
            bookRead.setBookEarningBasis(earningBasis);
            bookRead.setBasisLockedAt(java.time.LocalDateTime.now());
            
            // Handle page count for PER_PAGE_MILESTONE basis
            if (earningBasis == RewardEarningBasis.PER_PAGE_MILESTONE) {
                if (totalPagesOverride != null) {
                    bookRead.setPageCount(totalPagesOverride);
                    bookRead.setPageCountConfirmed(true);
                } else if (suggestedPageCount != null) {
                    bookRead.setPageCount(suggestedPageCount);
                    // Page count not confirmed until user confirms
                    bookRead.setPageCountConfirmed(pageCountConfirmed != null && pageCountConfirmed);
                }
            }
            
            return bookReadRepository.save(bookRead);
        }
        
        throw new IllegalArgumentException("Book read not found for child");
    }

    /**
     * Confirm or override the page count for a PER_PAGE_MILESTONE basis.
     */
    @Transactional
    public BookRead confirmOrOverridePageCount(UUID childId, String googleBookId, 
                                               Integer totalPages, Boolean confirmed) {
        Optional<BookRead> bookRead = bookReadRepository.findByUserIdAndGoogleBookIdAndEndDateIsNull(childId, googleBookId);
        
        if (bookRead.isEmpty()) {
            throw new IllegalArgumentException("Book read not found");
        }
        
        BookRead br = bookRead.get();
        
        // Only valid for PER_PAGE_MILESTONE basis
        if (br.getBookEarningBasis() != RewardEarningBasis.PER_PAGE_MILESTONE) {
            throw new IllegalStateException("Page count confirmation only valid for PER_PAGE_MILESTONE basis");
        }
        
        br.setPageCount(totalPages);
        br.setPageCountConfirmed(confirmed);
        
        return bookReadRepository.save(br);
    }

    /**
     * Update reading progress based on tracking mode and calculate earnings.
     * Supports CHAPTERS mode (chapters_completed) and PAGES mode (current_page with milestone logic).
     */
    @Transactional
    public BookRead updateReadingProgress(UUID childId, String googleBookId, 
                                         ReadingTrackingMode trackingMode,
                                         Integer chaptersCompleted,
                                         Integer currentPage) {
        Optional<BookRead> bookRead = bookReadRepository.findByUserIdAndGoogleBookIdAndEndDateIsNull(childId, googleBookId);
        
        if (bookRead.isEmpty()) {
            throw new IllegalArgumentException("Book read not found");
        }
        
        BookRead br = bookRead.get();
        
        // Update tracking fields based on mode
        if (trackingMode == ReadingTrackingMode.CHAPTERS && chaptersCompleted != null) {
            br.setChaptersCompleted(chaptersCompleted);
            br.setTrackingMode(ReadingTrackingMode.CHAPTERS);
        } else if (trackingMode == ReadingTrackingMode.PAGES && currentPage != null) {
            br.setCurrentPage(currentPage);
            br.setTrackingMode(ReadingTrackingMode.PAGES);
        }
        
        return bookReadRepository.save(br);
    }

    /**
     * Calculate earnings for page milestone basis when pages reach a threshold.
     * Implements carry-forward logic: earns only at full threshold completion.
     * 
     * Returns the amount earned, or 0 if threshold not reached.
     */
    public double calculatePageMilestoneEarnings(BookRead bookRead, RewardOption rewardOption, int newPageCount) {
        // Validate requirements
        if (bookRead.getBookEarningBasis() != RewardEarningBasis.PER_PAGE_MILESTONE) {
            return 0;
        }
        if (rewardOption.getPageMilestoneSize() == null || rewardOption.getPageMilestoneSize() <= 0) {
            return 0;
        }
        if (!Boolean.TRUE.equals(bookRead.getPageCountConfirmed())) {
            // Cannot earn until page count is confirmed
            return 0;
        }
        
        int milestoneSize = rewardOption.getPageMilestoneSize();
        int carryForward = bookRead.getPageMilestoneCarryForward();
        int totalPagesForEarning = carryForward + newPageCount;
        
        // Calculate how many full milestones we've completed
        int milestonesCompleted = totalPagesForEarning / milestoneSize;
        int newCarryForward = totalPagesForEarning % milestoneSize;
        
        // Update carry forward in the database
        bookRead.setPageMilestoneCarryForward(newCarryForward);
        bookReadRepository.save(bookRead);
        
        // Calculate earnings: one unit per milestone
        if (rewardOption.getValueType() == RewardValueType.MONEY) {
            return rewardOption.getMoneyAmount() * milestonesCompleted;
        } else if (rewardOption.getValueType() == RewardValueType.NON_MONEY) {
            return rewardOption.getNonMoneyQuantity() * milestonesCompleted;
        }
        
        return 0;
    }

    /**
     * Validate that tracking mode is compatible with the assignment's earning basis.
     */
    public boolean isTrackingModeCompatible(ReadingTrackingMode trackingMode, RewardEarningBasis earningBasis) {
        switch (earningBasis) {
            case PER_BOOK:
                // PER_BOOK only works with BOOK_ONLY tracking
                return trackingMode == ReadingTrackingMode.BOOK_ONLY;
            case PER_CHAPTER:
                // PER_CHAPTER requires CHAPTERS tracking
                return trackingMode == ReadingTrackingMode.CHAPTERS;
            case PER_PAGE_MILESTONE:
                // PER_PAGE_MILESTONE requires PAGES tracking
                return trackingMode == ReadingTrackingMode.PAGES;
            default:
                return false;
        }
    }
}
