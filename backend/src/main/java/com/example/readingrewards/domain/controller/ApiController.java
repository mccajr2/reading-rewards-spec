package com.example.readingrewards.domain.controller;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.domain.dto.BookReadProgressDto;
import com.example.readingrewards.domain.dto.BookSummaryDto;
import com.example.readingrewards.domain.dto.HistoryItemDto;
import com.example.readingrewards.domain.model.*;
import com.example.readingrewards.domain.repo.*;
import com.example.readingrewards.domain.service.GoogleBooksService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final GoogleBooksService googleBooksService;
    private final BookRepository bookRepo;
    private final ChapterRepository chapterRepo;
    private final ChapterReadRepository chapterReadRepo;
    private final BookReadRepository bookReadRepo;
    private final UserRepository userRepo;
    private final RewardRepository rewardRepo;

    public ApiController(GoogleBooksService googleBooksService,
                         BookRepository bookRepo,
                         ChapterRepository chapterRepo,
                         ChapterReadRepository chapterReadRepo,
                         BookReadRepository bookReadRepo,
                         UserRepository userRepo,
                         RewardRepository rewardRepo) {
        this.googleBooksService = googleBooksService;
        this.bookRepo = bookRepo;
        this.chapterRepo = chapterRepo;
        this.chapterReadRepo = chapterReadRepo;
        this.bookReadRepo = bookReadRepo;
        this.userRepo = userRepo;
        this.rewardRepo = rewardRepo;
    }

    private User getCurrentUser(UserDetails userDetails) {
        String identifier = userDetails.getUsername();
        if (identifier.contains("@")) {
            return userRepo.findByEmail(identifier)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        return userRepo.findByUsername(identifier)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ---- Search ----

    @GetMapping("/search")
    public List<BookSummaryDto> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String isbn) {
        return googleBooksService.search(title, author, isbn);
    }

    // ---- Books / Reading List ----

    @Transactional
    @GetMapping("/books")
    public List<Map<String, Object>> getBooks(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<BookRead> bookReads = bookReadRepo.findByUserId(user.getId());
        Map<String, Map<String, Object>> bookMap = new LinkedHashMap<>();
        for (BookRead br : bookReads) {
            Book book = br.getBook();
            if (book == null) continue;
            String id = book.getGoogleBookId();
            Map<String, Object> m = bookMap.computeIfAbsent(id, k -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("googleBookId", book.getGoogleBookId());
                entry.put("title", book.getTitle());
                entry.put("description", book.getDescription());
                entry.put("thumbnailUrl", book.getThumbnailUrl());
                entry.put("authors", book.getAuthors());
                entry.put("inProgress", br.isInProgress());
                entry.put("readCount", 0);
                entry.put("endDate", LocalDateTime.MIN);
                return entry;
            });
            m.put("readCount", (int) m.get("readCount") + 1);
            if (br.getEndDate() != null) {
                LocalDateTime current = (LocalDateTime) m.get("endDate");
                if (br.getEndDate().isAfter(current)) m.put("endDate", br.getEndDate());
            }
        }
        return new ArrayList<>(bookMap.values());
    }

    @PostMapping("/books")
    public Map<String, Object> saveBook(@RequestBody BookSummaryDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Book book = bookRepo.findById(dto.getGoogleBookId()).orElseGet(Book::new);
        book.setGoogleBookId(dto.getGoogleBookId());
        book.setTitle(dto.getTitle());
        book.setDescription(dto.getDescription());
        book.setThumbnailUrl(dto.getThumbnailUrl());
        book.setAuthors(dto.getAuthors() != null ? dto.getAuthors() : List.of());
        bookRepo.save(book);
        BookRead br = new BookRead();
        br.setGoogleBookId(book.getGoogleBookId());
        br.setUserId(user.getId());
        br.setStartDate(LocalDateTime.now());
        BookRead savedBr = bookReadRepo.save(br);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", savedBr.getId());
        result.put("googleBookId", savedBr.getGoogleBookId());
        result.put("title", book.getTitle());
        result.put("userId", savedBr.getUserId());
        result.put("startDate", savedBr.getStartDate());
        return result;
    }

    @PostMapping("/books/{googleBookId}/finish")
    @Transactional
    public ResponseEntity<?> finishBook(@PathVariable String googleBookId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<BookRead> bookReads = bookReadRepo.findByUserId(user.getId());
        boolean found = false;
        for (BookRead br : bookReads) {
            if (googleBookId.equals(br.getGoogleBookId()) && br.isInProgress()) {
                br.setEndDate(LocalDateTime.now());
                bookReadRepo.save(br);
                found = true;
            }
        }
        return found ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/books/{googleBookId}/reread")
    public ResponseEntity<BookRead> rereadBook(@PathVariable String googleBookId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (bookRepo.findById(googleBookId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        BookRead br = new BookRead();
        br.setGoogleBookId(googleBookId);
        br.setUserId(user.getId());
        br.setStartDate(LocalDateTime.now());
        return ResponseEntity.ok(bookReadRepo.save(br));
    }

    @DeleteMapping("/bookreads/{bookReadId}")
    @Transactional
    public ResponseEntity<?> deleteBookRead(@PathVariable UUID bookReadId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Optional<BookRead> opt = bookReadRepo.findById(bookReadId);
        if (opt.isEmpty() || !user.getId().equals(opt.get().getUserId())) {
            return ResponseEntity.notFound().build();
        }
        List<ChapterRead> chapterReads = chapterReadRepo.findByBookReadId(bookReadId);
        for (ChapterRead cr : chapterReads) {
            List<Reward> rewards = rewardRepo.findByUserId(user.getId()).stream()
                    .filter(r -> cr.getId().equals(r.getChapterReadId()))
                    .collect(Collectors.toList());
            rewardRepo.deleteAll(rewards);
        }
        chapterReadRepo.deleteAll(chapterReads);
        bookReadRepo.deleteById(bookReadId);
        return ResponseEntity.ok().build();
    }

    // ---- Chapters ----

    @GetMapping("/books/{googleBookId}/chapters")
    public List<Chapter> getChapters(@PathVariable String googleBookId) {
        return chapterRepo.findByGoogleBookIdOrderByChapterIndex(googleBookId);
    }

    @GetMapping("/bookreads/{bookReadId}/chapters")
    public List<Chapter> getChaptersForBookRead(@PathVariable UUID bookReadId) {
        Optional<BookRead> br = bookReadRepo.findById(bookReadId);
        if (br.isEmpty()) return Collections.emptyList();
        return chapterRepo.findByGoogleBookIdOrderByChapterIndex(br.get().getGoogleBookId());
    }

    @PostMapping("/books/{googleBookId}/chapters")
    public List<Chapter> saveChapters(@PathVariable String googleBookId,
                                      @RequestBody List<Chapter> chapters) {
        List<Chapter> existing = chapterRepo.findByGoogleBookIdOrderByChapterIndex(googleBookId);
        if (!existing.isEmpty()) {
            return existing;
        }
        chapters.forEach(c -> c.setGoogleBookId(googleBookId));
        return chapterRepo.saveAll(chapters);
    }

    @PostMapping("/bookreads/{bookReadId}/chapters")
    public List<Chapter> saveChaptersForBookRead(@PathVariable UUID bookReadId,
                                                 @RequestBody List<Chapter> chapters) {
        Optional<BookRead> br = bookReadRepo.findById(bookReadId);
        if (br.isEmpty()) return Collections.emptyList();
        String googleBookId = br.get().getGoogleBookId();
        List<Chapter> existing = chapterRepo.findByGoogleBookIdOrderByChapterIndex(googleBookId);
        if (!existing.isEmpty()) {
            return existing;
        }
        chapters.forEach(c -> c.setGoogleBookId(googleBookId));
        return chapterRepo.saveAll(chapters);
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<Chapter> renameChapter(@PathVariable UUID id,
                                                 @RequestBody Map<String, String> body) {
        Optional<Chapter> opt = chapterRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        String newName = body.get("name");
        if (newName == null || newName.isBlank()) return ResponseEntity.badRequest().build();
        Chapter chapter = opt.get();
        chapter.setName(newName.trim());
        return ResponseEntity.ok(chapterRepo.save(chapter));
    }

    // ---- Chapter Reads (mark/unmark) ----

    @PostMapping("/bookreads/{bookReadId}/chapters/{chapterId}/read")
    public ResponseEntity<?> markChapterRead(@PathVariable UUID bookReadId,
                                             @PathVariable UUID chapterId,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);

        Optional<ChapterRead> existing = chapterReadRepo.findByBookReadIdAndChapterIdAndUserId(bookReadId, chapterId, user.getId());
        if (existing.isPresent()) {
            return ResponseEntity.ok().build();
        }

        ChapterRead cr = new ChapterRead();
        cr.setBookReadId(bookReadId);
        cr.setChapterId(chapterId);
        cr.setUserId(user.getId());
        cr.setCompletionDate(LocalDateTime.now());
        ChapterRead saved = chapterReadRepo.save(cr);

        Reward reward = new Reward();
        reward.setType(RewardType.EARN);
        reward.setUserId(user.getId());
        reward.setChapterReadId(saved.getId());
        reward.setAmount(1.0);
        rewardRepo.save(reward);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/books/{googleBookId}/chapters/{chapterId}/read")
    public ResponseEntity<?> deleteChapterRead(@PathVariable String googleBookId,
                                               @PathVariable UUID chapterId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<ChapterRead> reads = chapterReadRepo.findByUserId(user.getId()).stream()
                .filter(r -> chapterId.equals(r.getChapterId()))
                .collect(Collectors.toList());
        if (reads.isEmpty()) return ResponseEntity.notFound().build();
        ChapterRead cr = reads.get(0);
        List<Reward> rewards = rewardRepo.findByUserId(user.getId()).stream()
                .filter(r -> cr.getId().equals(r.getChapterReadId()))
                .collect(Collectors.toList());
        rewardRepo.deleteAll(rewards);
        chapterReadRepo.delete(cr);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bookreads/{bookReadId}/chapterreads")
    public List<ChapterRead> getChapterReadsForBookRead(@PathVariable UUID bookReadId) {
        return chapterReadRepo.findByBookReadId(bookReadId);
    }

    @Transactional
    @GetMapping("/bookreads/in-progress")
    public List<BookReadProgressDto> getInProgressBookReads(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<BookRead> bookReads = bookReadRepo.findByUserId(user.getId());
        List<BookReadProgressDto> result = new ArrayList<>();
        for (BookRead br : bookReads) {
            if (!br.isInProgress()) continue;
            Book book = br.getBook();
            if (book == null) continue;
            int readCount = (int) bookReads.stream()
                    .filter(b -> b.getGoogleBookId().equals(book.getGoogleBookId()))
                    .count();
            List<UUID> readChapterIds = chapterReadRepo.findByBookReadId(br.getId())
                    .stream().map(ChapterRead::getChapterId).toList();
            result.add(new BookReadProgressDto(br, book, readCount, readChapterIds));
        }
        return result;
    }

    // ---- History ----

    @Transactional
    @GetMapping("/history")
    public List<HistoryItemDto> history(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return chapterReadRepo.findByUserId(user.getId()).stream()
            .map(cr -> new HistoryItemDto(
                cr.getId(),
                cr.getChapterId(),
                cr.getChapter() != null ? cr.getChapter().getName() : null,
                cr.getBookRead() != null && cr.getBookRead().getBook() != null
                    ? cr.getBookRead().getBook().getTitle() : null,
                cr.getCompletionDate()
            ))
            .collect(Collectors.toList());
    }

    // ---- Credits ----

    @GetMapping("/credits")
    public Map<String, Object> credits(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<Reward> rewards = rewardRepo.findByUserId(user.getId());
        int totalCents = (int) rewards.stream()
                .filter(r -> r.getType() == RewardType.EARN)
                .count() * 100;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cents", totalCents);
        m.put("dollars", totalCents / 100.0);
        return m;
    }

    // ---- Rewards ----

    @GetMapping("/rewards/summary")
    public Map<String, Object> getRewardsSummary(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        double earned = rewardRepo.getTotalEarnedByUserId(user.getId());
        double paidOut = rewardRepo.getTotalPaidOutByUserId(user.getId());
        double spent = rewardRepo.getTotalSpentByUserId(user.getId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalEarned", earned);
        m.put("totalPaidOut", paidOut);
        m.put("totalSpent", spent);
        m.put("currentBalance", earned - paidOut - spent);
        return m;
    }

    @GetMapping("/rewards")
    public Map<String, Object> getRewards(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<Reward> allRewards = rewardRepo.findByUserId(user.getId());
        allRewards.sort(Comparator.comparing(Reward::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        int total = allRewards.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to = Math.min(from + pageSize, total);
        List<Map<String, Object>> result = allRewards.subList(from, to)
                .stream().map(this::rewardToMap).toList();
        return Map.of("rewards", result, "totalCount", total);
    }

    @PostMapping("/rewards/spend")
    public ResponseEntity<?> spendReward(@RequestParam double amount,
                                         @RequestParam String note,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        if (amount <= 0) return ResponseEntity.badRequest().body("Amount must be positive");
        User user = getCurrentUser(userDetails);
        Reward reward = new Reward();
        reward.setType(RewardType.SPEND);
        reward.setUserId(user.getId());
        reward.setAmount(amount);
        reward.setNote(note);
        rewardRepo.save(reward);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rewards/payout")
    public ResponseEntity<?> payoutReward(@RequestParam double amount,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        if (amount <= 0) return ResponseEntity.badRequest().body("Amount must be positive");
        User user = getCurrentUser(userDetails);
        Reward reward = new Reward();
        reward.setType(RewardType.PAYOUT);
        reward.setUserId(user.getId());
        reward.setAmount(amount);
        reward.setNote("Payout");
        rewardRepo.save(reward);
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> rewardToMap(Reward reward) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", reward.getId());
        m.put("type", reward.getType());
        m.put("amount", reward.getAmount());
        m.put("note", reward.getNote());
        m.put("createdAt", reward.getCreatedAt());
        if (reward.getType() == RewardType.EARN && reward.getChapterRead() != null) {
            ChapterRead cr = reward.getChapterRead();
            m.put("chapterReadId", cr.getId());
            m.put("completionDate", cr.getCompletionDate());
            if (cr.getChapter() != null) {
                Chapter ch = cr.getChapter();
                m.put("chapter", Map.of(
                    "id", ch.getId(),
                    "name", ch.getName(),
                    "chapterIndex", ch.getChapterIndex(),
                    "bookGoogleBookId", ch.getGoogleBookId()
                ));
            }
            if (cr.getBookRead() != null) {
                BookRead br = cr.getBookRead();
                Map<String, Object> brMap = new LinkedHashMap<>();
                brMap.put("id", br.getId());
                brMap.put("startDate", br.getStartDate());
                brMap.put("endDate", br.getEndDate());
                brMap.put("inProgress", br.isInProgress());
                if (br.getBook() != null) {
                    Book book = br.getBook();
                    brMap.put("book", Map.of(
                        "googleBookId", book.getGoogleBookId(),
                        "title", book.getTitle(),
                        "authors", book.getAuthors() != null ? book.getAuthors() : List.of()
                    ));
                }
                m.put("bookRead", brMap);
            }
        }
        return m;
    }
}
