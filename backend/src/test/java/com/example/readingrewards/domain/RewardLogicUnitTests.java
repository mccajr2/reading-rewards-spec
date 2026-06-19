package com.example.readingrewards.domain;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.controller.ApiController;
import com.example.readingrewards.domain.model.ChapterRead;
import com.example.readingrewards.domain.model.Reward;
import com.example.readingrewards.domain.model.RewardType;
import com.example.readingrewards.domain.repo.BookReadRepository;
import com.example.readingrewards.domain.repo.BookRepository;
import com.example.readingrewards.domain.repo.ChapterReadRepository;
import com.example.readingrewards.domain.repo.ChapterRepository;
import com.example.readingrewards.domain.repo.RewardRepository;
import com.example.readingrewards.domain.service.GoogleBooksService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardLogicUnitTests {

    @Mock
    private GoogleBooksService googleBooksService;

    @Mock
    private BookRepository bookRepo;

    @Mock
    private ChapterRepository chapterRepo;

    @Mock
    private ChapterReadRepository chapterReadRepo;

    @Mock
    private BookReadRepository bookReadRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private RewardRepository rewardRepo;

    @InjectMocks
    private ApiController apiController;

    @Test
    void markChapterRead_createsChapterReadAndEarnReward() {
        UUID userId = UUID.randomUUID();
        UUID bookReadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID chapterReadId = UUID.randomUUID();

        User user = buildChildUser(userId, "kid-user");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername("kid-user")
            .password("ignored")
            .roles("CHILD")
            .build();

        when(userRepo.findByUsername("kid-user")).thenReturn(Optional.of(user));
        when(chapterReadRepo.findByBookReadIdAndChapterIdAndUserId(bookReadId, chapterId, userId))
            .thenReturn(Optional.empty());
        when(bookReadRepo.findById(bookReadId)).thenReturn(Optional.empty());
        when(chapterReadRepo.save(any(ChapterRead.class))).thenAnswer(invocation -> {
            ChapterRead cr = invocation.getArgument(0);
            cr.setId(chapterReadId);
            return cr;
        });

        var response = apiController.markChapterRead(bookReadId, chapterId, null, userDetails);

        assertEquals(200, response.getStatusCode().value());

        verify(chapterReadRepo, times(1)).save(any(ChapterRead.class));
        verify(rewardRepo, never()).save(any(Reward.class));
    }

    @Test
    void markChapterRead_duplicateDoesNotCreateSideEffects() {
        UUID userId = UUID.randomUUID();
        UUID bookReadId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();

        User user = buildChildUser(userId, "kid-user");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername("kid-user")
            .password("ignored")
            .roles("CHILD")
            .build();

        ChapterRead existing = new ChapterRead();
        existing.setId(UUID.randomUUID());
        existing.setBookReadId(bookReadId);
        existing.setChapterId(chapterId);
        existing.setUserId(userId);

        when(userRepo.findByUsername("kid-user")).thenReturn(Optional.of(user));
        when(chapterReadRepo.findByBookReadIdAndChapterIdAndUserId(bookReadId, chapterId, userId))
            .thenReturn(Optional.of(existing));

        var response = apiController.markChapterRead(bookReadId, chapterId, null, userDetails);

        assertEquals(200, response.getStatusCode().value());
        verify(chapterReadRepo, never()).save(any(ChapterRead.class));
        verify(rewardRepo, never()).save(any(Reward.class));
    }

    @Test
    void deleteChapterRead_deletesLinkedRewardsAndChapterRead() {
        UUID userId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        UUID chapterReadId = UUID.randomUUID();

        User user = buildChildUser(userId, "kid-user");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername("kid-user")
            .password("ignored")
            .roles("CHILD")
            .build();

        ChapterRead read = new ChapterRead();
        read.setId(chapterReadId);
        read.setChapterId(chapterId);
        read.setUserId(userId);

        Reward linkedReward = new Reward();
        linkedReward.setId(UUID.randomUUID());
        linkedReward.setUserId(userId);
        linkedReward.setChapterReadId(chapterReadId);

        Reward unrelatedReward = new Reward();
        unrelatedReward.setId(UUID.randomUUID());
        unrelatedReward.setUserId(userId);
        unrelatedReward.setChapterReadId(UUID.randomUUID());

        when(userRepo.findByUsername("kid-user")).thenReturn(Optional.of(user));
        when(chapterReadRepo.findByUserId(userId)).thenReturn(List.of(read));
        when(rewardRepo.findByUserId(userId)).thenReturn(List.of(linkedReward, unrelatedReward));

        var response = apiController.deleteChapterRead("book-1", chapterId, userDetails);

        assertEquals(200, response.getStatusCode().value());

        ArgumentCaptor<List<Reward>> rewardsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rewardRepo, times(1)).deleteAll(rewardsCaptor.capture());
        List<Reward> deletedRewards = rewardsCaptor.getValue();

        assertEquals(1, deletedRewards.size());
        assertEquals(linkedReward.getId(), deletedRewards.get(0).getId());
        assertFalse(deletedRewards.stream().anyMatch(r -> r.getId().equals(unrelatedReward.getId())));
        verify(chapterReadRepo, times(1)).delete(read);
    }

    @Test
    void deleteChapterRead_returnsNotFoundWhenMissing() {
        UUID userId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();

        User user = buildChildUser(userId, "kid-user");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
            .withUsername("kid-user")
            .password("ignored")
            .roles("CHILD")
            .build();

        when(userRepo.findByUsername("kid-user")).thenReturn(Optional.of(user));
        when(chapterReadRepo.findByUserId(userId)).thenReturn(List.of());

        var response = apiController.deleteChapterRead("book-1", chapterId, userDetails);

        assertEquals(404, response.getStatusCode().value());
        verify(rewardRepo, never()).deleteAll(any());
        verify(chapterReadRepo, never()).delete(any(ChapterRead.class));
    }

    private static User buildChildUser(UUID id, String username) {
        User user = new User();
        user.setId(id);
        user.setRole(User.UserRole.CHILD);
        user.setUsername(username);
        return user;
    }

}
