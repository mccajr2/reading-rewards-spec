package com.example.readingrewards.domain.dto;

import com.example.readingrewards.domain.model.RewardType;
import com.example.readingrewards.domain.model.RewardEarningBasis;
import com.example.readingrewards.domain.model.RewardScopeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class BookSummaryDto {
    private String googleBookId;
    private String title;
    private List<String> authors;
    private String description;
    private String thumbnailUrl;

    public BookSummaryDto() {}

    public BookSummaryDto(String googleBookId, String title, List<String> authors,
                          String description, String thumbnailUrl) {
        this.googleBookId = googleBookId;
        this.title = title;
        this.authors = authors;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getGoogleBookId() { return googleBookId; }
    public void setGoogleBookId(String googleBookId) { this.googleBookId = googleBookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public record BookRollupDto(
        String googleBookId,
        String title,
        String description,
        String thumbnailUrl,
        List<String> authors,
        boolean inProgress,
        int readCount,
        LocalDateTime endDate
    ) {}

    public record SavedBookDto(
        UUID id,
        String googleBookId,
        String title,
        UUID userId,
        LocalDateTime startDate
    ) {}

    public record CreditsDto(int cents, double dollars) {}

    public record RewardRollupDto(
        double totalEarned,
        double totalPaidOut,
        double totalSpent,
        double currentBalance
    ) {}

    public record RewardHistoryPageDto(List<RewardHistoryItemDto> rewards, int totalCount) {}

    public record RewardHistoryItemDto(
        UUID id,
        RewardType type,
        double amount,
        String note,
        LocalDateTime createdAt,
        UUID rewardOptionId,
        String rewardOptionName,
        RewardEarningBasis rewardOptionBasis,
        UUID chapterReadId,
        LocalDateTime completionDate,
        ChapterRefDto chapter,
        BookReadRefDto bookRead
    ) {}

    public record RewardOptionDto(
        UUID id,
        UUID ownerUserId,
        UUID childUserId,
        RewardScopeType scopeType,
        String name,
        String description,
        RewardEarningBasis earningBasis,
        double amount,
        Integer pageMilestoneSize,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {}

    public record RewardOptionsResponseDto(
        List<RewardOptionDto> options,
        UUID activeSelectionId,
        UUID activeSelectionOptionId
    ) {}

    public record ChapterRefDto(
        UUID id,
        String name,
        Integer chapterIndex,
        String bookGoogleBookId
    ) {}

    public record BookReadRefDto(
        UUID id,
        LocalDateTime startDate,
        LocalDateTime endDate,
        boolean inProgress,
        BookRefDto book
    ) {}

    public record BookRefDto(String googleBookId, String title, List<String> authors) {}
}
