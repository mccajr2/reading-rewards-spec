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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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
    private final RewardOptionRepository rewardOptionRepo;
    private final ChildRewardSelectionRepository rewardSelectionRepo;

    public ApiController(GoogleBooksService googleBooksService,
                         BookRepository bookRepo,
                         ChapterRepository chapterRepo,
                         ChapterReadRepository chapterReadRepo,
                         BookReadRepository bookReadRepo,
                         UserRepository userRepo,
                         RewardRepository rewardRepo,
                         RewardOptionRepository rewardOptionRepo,
                         ChildRewardSelectionRepository rewardSelectionRepo) {
        this.googleBooksService = googleBooksService;
        this.bookRepo = bookRepo;
        this.chapterRepo = chapterRepo;
        this.chapterReadRepo = chapterReadRepo;
        this.bookReadRepo = bookReadRepo;
        this.userRepo = userRepo;
        this.rewardRepo = rewardRepo;
        this.rewardOptionRepo = rewardOptionRepo;
        this.rewardSelectionRepo = rewardSelectionRepo;
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

    private User requireParent(User user) {
        if (user.getRole() != User.UserRole.PARENT) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Parent access required");
        }
        return user;
    }

    private User requireChild(User user) {
        if (user.getRole() != User.UserRole.CHILD) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Child access required");
        }
        return user;
    }

    private RewardOption createDefaultFamilyOption(User parent) {
        RewardOption rewardOption = new RewardOption();
        rewardOption.setOwnerUserId(parent.getId());
        rewardOption.setScopeType(RewardScopeType.FAMILY);
        rewardOption.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
        rewardOption.setValueType(RewardValueType.MONEY);
        rewardOption.setCurrencyCode("USD");
        rewardOption.setMoneyAmount(1.0);
        rewardOption.setActive(Boolean.TRUE);
        applyInferredRewardPresentation(rewardOption);
        return rewardOptionRepo.save(rewardOption);
    }

    private boolean isDefaultFamilyOption(RewardOption rewardOption) {
        return rewardOption != null
                && rewardOption.getScopeType() == RewardScopeType.FAMILY
                && rewardOption.getChildUserId() == null
                && rewardOption.getValueType() == RewardValueType.MONEY
                && rewardOption.getEarningBasis() == RewardEarningBasis.PER_CHAPTER
                && rewardOption.getPageMilestoneSize() == null
                && rewardOption.getMoneyAmount() != null
                && Double.compare(rewardOption.getMoneyAmount(), 1.0d) == 0;
    }

    private void validateDefinitionEditAllowed(RewardOption existing, Map<String, Object> body) {
        if (!isDefaultFamilyOption(existing)) {
            return;
        }
        boolean changesDefinition = body.keySet().stream().anyMatch(key -> !"active".equals(key));
        if (changesDefinition) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Default $1 per chapter reward cannot be edited");
        }
    }

    private void ensureCanDeactivateRewardOption(User parent, RewardOption option) {
        if (!Boolean.TRUE.equals(option.getActive())) {
            return;
        }
        long activeOptions = rewardOptionRepo.findByOwnerUserIdOrderByCreatedAtAsc(parent.getId()).stream()
                .filter(existing -> Boolean.TRUE.equals(existing.getActive()))
                .count();
        if (activeOptions <= 1) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "At least one reward option must remain active");
        }
    }

    private void deactivateSelectionsForOption(UUID rewardOptionId) {
        rewardSelectionRepo.findByRewardOptionIdAndActiveTrue(rewardOptionId).forEach(selection -> {
            selection.setActive(Boolean.FALSE);
            rewardSelectionRepo.save(selection);
        });
    }

    private void applyInferredRewardPresentation(RewardOption rewardOption) {
        rewardOption.setName(inferRewardOptionName(rewardOption));
        rewardOption.setDescription(null);
    }

    private String inferRewardOptionName(RewardOption rewardOption) {
        String valueLabel;
        if (rewardOption.getValueType() == RewardValueType.NON_MONEY) {
            valueLabel = formatNumber(rewardOption.getNonMoneyQuantity()) + " " + String.valueOf(rewardOption.getNonMoneyUnitLabel()).trim();
        } else {
            valueLabel = "$" + formatNumber(rewardOption.getMoneyAmount());
        }

        return switch (rewardOption.getEarningBasis()) {
            case PER_BOOK -> valueLabel + " per book";
            case PER_PAGE_MILESTONE -> valueLabel + " per " + rewardOption.getPageMilestoneSize() + " pages";
            case PER_CHAPTER -> valueLabel + " per chapter";
        };
    }

    private String formatNumber(Double value) {
        if (value == null) {
            return "0";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private RewardOption ensureDefaultFamilyOption(User parent) {
        List<RewardOption> familyOptions = rewardOptionRepo.findByOwnerUserIdAndScopeTypeAndActiveTrueOrderByCreatedAtAsc(
                parent.getId(), RewardScopeType.FAMILY);
        if (!familyOptions.isEmpty()) {
            return familyOptions.get(0);
        }
        List<RewardOption> allOptions = rewardOptionRepo.findByOwnerUserIdOrderByCreatedAtAsc(parent.getId());
        if (allOptions.isEmpty()) {
            return createDefaultFamilyOption(parent);
        }
        return null;
    }

    private boolean isVisibleToChild(User child, RewardOption option) {
        if (option == null || !Boolean.TRUE.equals(option.getActive())) {
            return false;
        }
        if (option.getScopeType() == RewardScopeType.FAMILY) {
            return child.getParentId() != null && child.getParentId().equals(option.getOwnerUserId());
        }
        return child.getParentId() != null
                && child.getParentId().equals(option.getOwnerUserId())
                && child.getId().equals(option.getChildUserId());
    }

    private RewardOption resolveVisibleSelection(User child) {
        Optional<ChildRewardSelection> activeSelection = rewardSelectionRepo.findByChildUserIdAndActiveTrue(child.getId());
        if (activeSelection.isPresent()) {
            UUID selectedOptionId = activeSelection.get().getRewardOptionId();
            RewardOption selected = selectedOptionId != null
                    ? rewardOptionRepo.findById(selectedOptionId).orElse(null)
                    : null;
            if (selected != null && isVisibleToChild(child, selected)) {
                return selected;
            }
        }

        if (child.getParentId() == null) {
            return null;
        }

        User parent = userRepo.findById(child.getParentId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Parent not found"));
        RewardOption fallback = ensureDefaultFamilyOption(parent);
        if (fallback == null) {
            return null;
        }

        rewardSelectionRepo.findByChildUserIdAndActiveTrue(child.getId()).ifPresent(existing -> {
            existing.setActive(Boolean.FALSE);
            rewardSelectionRepo.save(existing);
        });

        ChildRewardSelection selection = new ChildRewardSelection();
        selection.setChildUserId(child.getId());
        selection.setRewardOptionId(fallback.getId());
        selection.setActive(Boolean.TRUE);
        rewardSelectionRepo.save(selection);
        return fallback;
    }

    private RewardOption parseRewardOptionPayload(Map<String, Object> body, RewardOption existing, User parent) {
        RewardOption rewardOption = existing != null ? existing : new RewardOption();
        if (existing == null) {
            rewardOption.setOwnerUserId(parent.getId());
        }

        Object scopeType = body.get("scopeType");
        if (scopeType != null) {
            rewardOption.setScopeType(RewardScopeType.valueOf(String.valueOf(scopeType).toUpperCase(Locale.ROOT)));
        }

        Object earningBasis = body.get("earningBasis");
        if (earningBasis != null) {
            rewardOption.setEarningBasis(RewardEarningBasis.valueOf(String.valueOf(earningBasis).toUpperCase(Locale.ROOT)));
        }

        // Typed value model: parse valueType first, then type-specific fields
        Object valueTypeRaw = body.get("valueType");
        if (valueTypeRaw != null) {
            rewardOption.setValueType(RewardValueType.valueOf(String.valueOf(valueTypeRaw).toUpperCase(Locale.ROOT)));
        } else if (existing == null) {
            rewardOption.setValueType(RewardValueType.MONEY); // default for new options
        }

        if (rewardOption.getValueType() == RewardValueType.MONEY || rewardOption.getValueType() == null) {
            // Accept both "moneyAmount" and legacy "amount" keys
            Object moneyAmount = body.containsKey("moneyAmount") ? body.get("moneyAmount") : body.get("amount");
            if (moneyAmount != null) {
                rewardOption.setMoneyAmount(Double.parseDouble(String.valueOf(moneyAmount)));
            }
            String currencyCode = body.containsKey("currencyCode")
                    ? String.valueOf(body.get("currencyCode"))
                    : null;
            if (currencyCode != null && !currencyCode.isBlank()) {
                rewardOption.setCurrencyCode(currencyCode.toUpperCase(Locale.ROOT));
            } else if (rewardOption.getCurrencyCode() == null) {
                rewardOption.setCurrencyCode("USD");
            }
            rewardOption.setNonMoneyQuantity(null);
            rewardOption.setNonMoneyUnitLabel(null);
        } else {
            // NON_MONEY
            Object qty = body.get("nonMoneyQuantity");
            if (qty != null) {
                rewardOption.setNonMoneyQuantity(Double.parseDouble(String.valueOf(qty)));
            }
            Object unitLabel = body.get("nonMoneyUnitLabel");
            if (unitLabel != null) {
                rewardOption.setNonMoneyUnitLabel(String.valueOf(unitLabel).trim());
            }
            rewardOption.setMoneyAmount(null);
            rewardOption.setCurrencyCode(null);
        }

        if (body.containsKey("pageMilestoneSize")) {
            Object milestone = body.get("pageMilestoneSize");
            rewardOption.setPageMilestoneSize(milestone == null || String.valueOf(milestone).isBlank()
                    ? null
                    : Integer.parseInt(String.valueOf(milestone)));
        }

        if (body.containsKey("active")) {
            rewardOption.setActive(Boolean.parseBoolean(String.valueOf(body.get("active"))));
        }

        if (rewardOption.getScopeType() == RewardScopeType.CHILD) {
            Object childUserId = body.get("childUserId");
            if (childUserId == null && existing == null) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "childUserId is required for child-scoped reward options");
            }
            if (childUserId != null) {
                UUID childId = UUID.fromString(String.valueOf(childUserId));
                User child = userRepo.findById(childId)
                        .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Child not found"));
                if (child.getRole() != User.UserRole.CHILD || !parent.getId().equals(child.getParentId())) {
                    throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Reward option must target one of your children");
                }
                rewardOption.setChildUserId(childId);
            }
        } else {
            rewardOption.setChildUserId(null);
        }

        if (rewardOption.getValueType() == RewardValueType.MONEY) {
            if (rewardOption.getMoneyAmount() == null || rewardOption.getMoneyAmount() <= 0) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "moneyAmount must be positive for MONEY reward options");
            }
        } else if (rewardOption.getValueType() == RewardValueType.NON_MONEY) {
            if (rewardOption.getNonMoneyQuantity() == null || rewardOption.getNonMoneyQuantity() <= 0) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "nonMoneyQuantity must be positive for NON_MONEY reward options");
            }
            if (rewardOption.getNonMoneyUnitLabel() == null || rewardOption.getNonMoneyUnitLabel().isBlank()) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "nonMoneyUnitLabel is required for NON_MONEY reward options");
            }
        }
        if (rewardOption.getEarningBasis() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "earningBasis is required");
        }
        if (rewardOption.getScopeType() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "scopeType is required");
        }
        if (rewardOption.getScopeType() == RewardScopeType.CHILD && rewardOption.getChildUserId() == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "childUserId is required for child-scoped reward options");
        }
        if (rewardOption.getEarningBasis() == RewardEarningBasis.PER_PAGE_MILESTONE) {
            if (rewardOption.getPageMilestoneSize() == null || rewardOption.getPageMilestoneSize() < 1) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "pageMilestoneSize is required for page milestone rewards");
            }
        } else {
            rewardOption.setPageMilestoneSize(null);
        }

        applyInferredRewardPresentation(rewardOption);

        return rewardOption;
    }

    private BookSummaryDto.RewardOptionDto rewardOptionToDto(RewardOption rewardOption) {
        return new BookSummaryDto.RewardOptionDto(
                rewardOption.getId(),
                rewardOption.getOwnerUserId(),
                rewardOption.getChildUserId(),
                rewardOption.getScopeType(),
                inferRewardOptionName(rewardOption),
                null,
                rewardOption.getValueType(),
                rewardOption.getCurrencyCode(),
                rewardOption.getMoneyAmount(),
                rewardOption.getNonMoneyQuantity(),
                rewardOption.getNonMoneyUnitLabel(),
                rewardOption.getEarningBasis(),
                rewardOption.getPageMilestoneSize(),
                Boolean.TRUE.equals(rewardOption.getActive()),
                rewardOption.getCreatedAt(),
                rewardOption.getUpdatedAt()
        );
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
    public List<BookSummaryDto.BookRollupDto> getBooks(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<BookRead> bookReads = bookReadRepo.findByUserId(user.getId());
        Map<String, BookAccumulator> bookMap = new LinkedHashMap<>();
        for (BookRead br : bookReads) {
            Book book = br.getBook();
            if (book == null) continue;
            String id = book.getGoogleBookId();
            BookAccumulator accumulator = bookMap.computeIfAbsent(id, k -> new BookAccumulator(
                    book.getGoogleBookId(),
                    book.getTitle(),
                    book.getDescription(),
                    book.getThumbnailUrl(),
                    book.getAuthors(),
                    br.isInProgress(),
                    0,
                    LocalDateTime.MIN
            ));
            accumulator.readCount += 1;
            if (br.getEndDate() != null) {
                LocalDateTime current = accumulator.endDate;
                if (br.getEndDate().isAfter(current)) {
                    accumulator.endDate = br.getEndDate();
                }
            }
        }
        return bookMap.values().stream()
                .map(item -> new BookSummaryDto.BookRollupDto(
                        item.googleBookId,
                        item.title,
                        item.description,
                        item.thumbnailUrl,
                        item.authors,
                        item.inProgress,
                        item.readCount,
                        item.endDate
                ))
                .toList();
    }

    @PostMapping("/books")
    public BookSummaryDto.SavedBookDto saveBook(@RequestBody BookSummaryDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Book book = bookRepo.findById(dto.getGoogleBookId()).orElseGet(Book::new);
        book.setGoogleBookId(dto.getGoogleBookId());
        book.setTitle(dto.getTitle());
        book.setDescription(dto.getDescription());
        book.setPageCount(dto.getPageCount());
        book.setThumbnailUrl(dto.getThumbnailUrl());
        book.setAuthors(dto.getAuthors() != null ? dto.getAuthors() : List.of());
        bookRepo.save(book);
        Optional<BookRead> existingInProgress = bookReadRepo.findByUserIdAndGoogleBookIdAndEndDateIsNull(
            user.getId(),
            book.getGoogleBookId()
        );
        if (existingInProgress.isPresent()) {
            BookRead existing = existingInProgress.get();
            if (existing.getPageCount() == null && dto.getPageCount() != null) {
                existing.setPageCount(dto.getPageCount());
                existing.setPageCountConfirmed(Boolean.TRUE);
                existing = bookReadRepo.save(existing);
            }
            if (existing.getBookEarningBasis() == null && dto.getEarningBasis() != null) {
                existing.setBookEarningBasis(dto.getEarningBasis());
                existing.setBasisLockedAt(LocalDateTime.now());
                existing = bookReadRepo.save(existing);
            }
            return new BookSummaryDto.SavedBookDto(
                existing.getId(),
                existing.getGoogleBookId(),
                book.getTitle(),
                existing.getUserId(),
                existing.getStartDate(),
                existing.getBookEarningBasis()
            );
        }
        BookRead br = new BookRead();
        br.setGoogleBookId(book.getGoogleBookId());
        br.setUserId(user.getId());
        br.setStartDate(LocalDateTime.now());
        if (dto.getPageCount() != null) {
            br.setPageCount(dto.getPageCount());
            br.setPageCountConfirmed(Boolean.TRUE);
        } else if (book.getPageCount() != null) {
            br.setPageCount(book.getPageCount());
            br.setPageCountConfirmed(Boolean.FALSE);
        }
        if (dto.getEarningBasis() != null) {
            br.setBookEarningBasis(dto.getEarningBasis());
            br.setBasisLockedAt(LocalDateTime.now());
        }
        BookRead savedBr = bookReadRepo.save(br);
        return new BookSummaryDto.SavedBookDto(
            savedBr.getId(),
            savedBr.getGoogleBookId(),
            book.getTitle(),
            savedBr.getUserId(),
            savedBr.getStartDate(),
            savedBr.getBookEarningBasis()
        );
    }

    @PutMapping("/children/{childId}/books/{bookId}/basis-selection")
    @Transactional
    public ResponseEntity<?> selectBookReadBasis(@PathVariable UUID childId,
                                                 @PathVariable String bookId,
                                                 @RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireChild(getCurrentUser(userDetails));
        if (!user.getId().equals(childId)) {
            return notFound("Book read not found for this user");
        }

        Optional<BookRead> opt = bookReadRepo.findByUserIdAndGoogleBookIdAndEndDateIsNull(childId, bookId);
        if (opt.isEmpty()) {
            return notFound("In-progress book read not found for this user and book");
        }

        BookRead br = opt.get();
        if (br.getBookEarningBasis() != null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                    .body(Map.of("error", "Book earning basis is already locked for this in-progress book"));
        }

        Object basisRaw = body.get("earningBasis");
        if (basisRaw == null || String.valueOf(basisRaw).isBlank()) {
            return badRequest("earningBasis is required");
        }

        RewardEarningBasis basis;
        try {
            basis = RewardEarningBasis.valueOf(String.valueOf(basisRaw).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return badRequest("Invalid earningBasis value");
        }

        Integer suggestedPageCount = null;
        if (body.containsKey("suggestedPageCount")) {
            Object rawPageCount = body.get("suggestedPageCount");
            if (rawPageCount != null && !String.valueOf(rawPageCount).isBlank()) {
                try {
                    suggestedPageCount = Integer.valueOf(String.valueOf(rawPageCount));
                } catch (NumberFormatException ex) {
                    return badRequest("Invalid suggestedPageCount value");
                }
            }
        }

        Integer totalPagesOverride = null;
        if (body.containsKey("totalPagesOverride")) {
            Object rawOverride = body.get("totalPagesOverride");
            if (rawOverride != null && !String.valueOf(rawOverride).isBlank()) {
                try {
                    totalPagesOverride = Integer.valueOf(String.valueOf(rawOverride));
                } catch (NumberFormatException ex) {
                    return badRequest("Invalid totalPagesOverride value");
                }
            }
        }

        // Support legacy "pageCount" parameter for backward compatibility
        Integer pageCount = null;
        if (body.containsKey("pageCount")) {
            Object rawPageCount = body.get("pageCount");
            if (rawPageCount != null && !String.valueOf(rawPageCount).isBlank()) {
                try {
                    pageCount = Integer.valueOf(String.valueOf(rawPageCount));
                } catch (NumberFormatException ex) {
                    return badRequest("Invalid pageCount value");
                }
            }
        }

        Boolean pageCountConfirmed = null;
        if (body.containsKey("pageCountConfirmed")) {
            Object rawConfirmed = body.get("pageCountConfirmed");
            if (rawConfirmed != null) {
                pageCountConfirmed = Boolean.parseBoolean(String.valueOf(rawConfirmed));
            }
        }

        // For PER_PAGE_MILESTONE, page count must be provided or confirmed
        if (basis == RewardEarningBasis.PER_PAGE_MILESTONE && totalPagesOverride == null && pageCount == null && suggestedPageCount == null) {
            return badRequest("pageCount is required for PER_PAGE_MILESTONE");
        }

        br.setBookEarningBasis(basis);
        br.setBasisLockedAt(LocalDateTime.now());

        // Handle page count for PER_PAGE_MILESTONE basis
        if (basis == RewardEarningBasis.PER_PAGE_MILESTONE) {
            // Priority: totalPagesOverride > pageCount (legacy) > suggestedPageCount
            if (totalPagesOverride != null) {
                br.setPageCount(totalPagesOverride);
                br.setPageCountConfirmed(Boolean.TRUE);
            } else if (pageCount != null) {
                br.setPageCount(pageCount);
                br.setPageCountConfirmed(Boolean.TRUE);
            } else if (suggestedPageCount != null) {
                br.setPageCount(suggestedPageCount);
                br.setPageCountConfirmed(pageCountConfirmed != null && pageCountConfirmed);
            }
        }

        BookRead saved = bookReadRepo.save(br);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookReadId", saved.getId());
        response.put("childId", saved.getUserId());
        response.put("bookId", saved.getGoogleBookId());
        response.put("bookEarningBasis", saved.getBookEarningBasis());
        response.put("pageCount", saved.getPageCount());
        response.put("pageCountConfirmed", saved.getPageCountConfirmed());
        response.put("basisLockedAt", saved.getBasisLockedAt());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/children/{childId}/books/{bookId}/page-count-confirmation")
    @Transactional
    public ResponseEntity<?> confirmPageCount(@PathVariable UUID childId,
                                              @PathVariable String bookId,
                                              @RequestBody Map<String, Object> body,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);

        // Allow child calling for themselves, or parent calling for their child
        if (user.getRole() == User.UserRole.CHILD) {
            if (!user.getId().equals(childId)) {
                return notFound("Book read not found for this user");
            }
        } else if (user.getRole() == User.UserRole.PARENT) {
            // Verify the target child belongs to this parent
            Optional<User> childOpt = userRepo.findById(childId);
            if (childOpt.isEmpty() || !user.getId().equals(childOpt.get().getParentId())) {
                return notFound("Child not found for this parent");
            }
        } else {
            return notFound("Book read not found for this user");
        }

        Optional<BookRead> opt = bookReadRepo.findByUserIdAndGoogleBookIdAndEndDateIsNull(childId, bookId);
        if (opt.isEmpty()) {
            return notFound("In-progress book read not found for this user and book");
        }

        BookRead br = opt.get();

        // Only valid for PER_PAGE_MILESTONE basis
        if (br.getBookEarningBasis() != RewardEarningBasis.PER_PAGE_MILESTONE) {
            return badRequest("Page count confirmation only valid for PER_PAGE_MILESTONE basis");
        }

        Object totalPagesRaw = body.get("totalPages");
        if (totalPagesRaw == null || String.valueOf(totalPagesRaw).isBlank()) {
            return badRequest("totalPages is required");
        }

        Object confirmedRaw = body.get("confirmed");
        if (confirmedRaw == null) {
            return badRequest("confirmed is required");
        }

        Integer totalPages;
        Boolean confirmed;
        try {
            totalPages = Integer.valueOf(String.valueOf(totalPagesRaw));
            confirmed = Boolean.parseBoolean(String.valueOf(confirmedRaw));
        } catch (NumberFormatException ex) {
            return badRequest("Invalid totalPages or confirmed value");
        }

        if (totalPages < 1) {
            return badRequest("totalPages must be at least 1");
        }

        br.setPageCount(totalPages);
        br.setPageCountConfirmed(confirmed);

        BookRead saved = bookReadRepo.save(br);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookReadId", saved.getId());
        response.put("childId", saved.getUserId());
        response.put("bookId", saved.getGoogleBookId());
        response.put("pageCount", saved.getPageCount());
        response.put("pageCountConfirmed", saved.getPageCountConfirmed());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/books/{googleBookId}/finish")
    @Transactional
    public ResponseEntity<?> finishBook(@PathVariable String googleBookId,
                                        @RequestBody(required = false) Map<String, Object> body,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireChild(getCurrentUser(userDetails));
        List<BookRead> bookReads = bookReadRepo.findByUserId(user.getId());
        boolean found = false;
        for (BookRead br : bookReads) {
            if (googleBookId.equals(br.getGoogleBookId()) && br.isInProgress()) {
                RewardOption completionOption = null;
                if (br.getBookEarningBasis() == RewardEarningBasis.PER_BOOK) {
                    User parent = userRepo.findById(user.getParentId())
                        .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Parent not found"));

                    List<RewardOption> eligibleOptions = rewardOptionRepo.findByOwnerUserIdOrderByCreatedAtAsc(parent.getId())
                        .stream()
                        .filter(option -> Boolean.TRUE.equals(option.getActive()))
                        .filter(option -> option.getEarningBasis() == RewardEarningBasis.PER_BOOK)
                        .filter(option -> isVisibleToChild(user, option))
                        .toList();

                    String requestedOptionId = body != null && body.get("rewardOptionId") != null
                        ? String.valueOf(body.get("rewardOptionId"))
                        : null;

                    if (requestedOptionId != null && !requestedOptionId.isBlank()) {
                        UUID requestedId = UUID.fromString(requestedOptionId);
                        completionOption = eligibleOptions.stream()
                            .filter(option -> requestedId.equals(option.getId()))
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                                "Selected reward option is not eligible for this completion event"));
                    } else if (eligibleOptions.size() == 1) {
                        completionOption = eligibleOptions.get(0);
                    } else if (eligibleOptions.size() > 1) {
                        List<Map<String, Object>> options = eligibleOptions.stream()
                            .map(option -> Map.<String, Object>of("id", option.getId(), "name", option.getName()))
                            .toList();
                        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(Map.of(
                            "error", "Multiple eligible reward options found; explicit completion selection is required",
                            "availableOptions", options
                        ));
                    }
                }

                br.setEndDate(LocalDateTime.now());
                bookReadRepo.save(br);
                found = true;
                if (completionOption != null) {
                    Reward reward = new Reward();
                    reward.setType(RewardType.EARN);
                    reward.setUserId(user.getId());
                    reward.setRewardOptionId(completionOption.getId());
                    reward.setAmount(completionOption.getEffectiveAmount());
                    reward.setNote(completionOption.getName());
                    rewardRepo.save(reward);
                }
            }
        }
        return found ? ResponseEntity.ok().build() : notFound("In-progress book read not found for this user");
    }

    @PostMapping("/books/{googleBookId}/reread")
    public ResponseEntity<?> rereadBook(@PathVariable String googleBookId,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (bookRepo.findById(googleBookId).isEmpty()) {
            return notFound("Book not found");
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
            return notFound("Book read not found for this user");
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
    public ResponseEntity<?> renameChapter(@PathVariable UUID id,
                                           @RequestBody Map<String, String> body) {
        Optional<Chapter> opt = chapterRepo.findById(id);
        if (opt.isEmpty()) return notFound("Chapter not found");
        String newName = body.get("name");
        if (newName == null || newName.isBlank()) return badRequest("Chapter name is required");
        Chapter chapter = opt.get();
        chapter.setName(newName.trim());
        return ResponseEntity.ok(chapterRepo.save(chapter));
    }

    // ---- Chapter Reads (mark/unmark) ----

    @PostMapping("/bookreads/{bookReadId}/chapters/{chapterId}/read")
    public ResponseEntity<?> markChapterRead(@PathVariable UUID bookReadId,
                                             @PathVariable UUID chapterId,
                                             @RequestBody(required = false) Map<String, Object> body,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireChild(getCurrentUser(userDetails));

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

        Optional<BookRead> bookReadOpt = bookReadRepo.findById(bookReadId);
        RewardOption completionOption = null;

        if (bookReadOpt.isPresent() && bookReadOpt.get().getBookEarningBasis() == RewardEarningBasis.PER_CHAPTER) {
            User parent = userRepo.findById(user.getParentId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Parent not found"));

            List<RewardOption> eligibleOptions = rewardOptionRepo.findByOwnerUserIdOrderByCreatedAtAsc(parent.getId())
                .stream()
                .filter(option -> Boolean.TRUE.equals(option.getActive()))
                .filter(option -> option.getEarningBasis() == RewardEarningBasis.PER_CHAPTER)
                .filter(option -> isVisibleToChild(user, option))
                .toList();

            String requestedOptionId = body != null && body.get("rewardOptionId") != null
                ? String.valueOf(body.get("rewardOptionId"))
                : null;

            if (requestedOptionId != null && !requestedOptionId.isBlank()) {
            UUID requestedId = UUID.fromString(requestedOptionId);
            completionOption = eligibleOptions.stream()
                .filter(option -> requestedId.equals(option.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Selected reward option is not eligible for this completion event"));
            } else if (eligibleOptions.size() == 1) {
            completionOption = eligibleOptions.get(0);
            } else if (eligibleOptions.size() > 1) {
            List<Map<String, Object>> options = eligibleOptions.stream()
                .map(option -> Map.<String, Object>of("id", option.getId(), "name", option.getName()))
                .toList();
            return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(Map.of(
                "error", "Multiple eligible reward options found; explicit completion selection is required",
                "availableOptions", options
            ));
            }
        }

        if (completionOption != null) {
            Reward reward = new Reward();
            reward.setType(RewardType.EARN);
            reward.setUserId(user.getId());
            reward.setChapterReadId(saved.getId());
            reward.setRewardOptionId(completionOption.getId());
            reward.setAmount(completionOption.getEffectiveAmount());
            reward.setNote(completionOption.getName());
            rewardRepo.save(reward);
        }

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
        if (reads.isEmpty()) return notFound("Chapter read not found for this user");
        ChapterRead cr = reads.get(0);
        List<Reward> rewards = rewardRepo.findByUserId(user.getId()).stream()
                .filter(r -> cr.getId().equals(r.getChapterReadId()))
                .collect(Collectors.toList());
        rewardRepo.deleteAll(rewards);
        chapterReadRepo.delete(cr);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bookreads/{bookReadId}/pages")
    @Transactional
    public ResponseEntity<?> logPageProgress(@PathVariable UUID bookReadId,
                                            @RequestBody Map<String, Object> body,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        User user = requireChild(getCurrentUser(userDetails));

        // Validate that currentPage is provided
        Object currentPageRaw = body.get("currentPage");
        if (currentPageRaw == null || String.valueOf(currentPageRaw).isBlank()) {
            return badRequest("currentPage is required");
        }

        Integer currentPage;
        try {
            currentPage = Integer.valueOf(String.valueOf(currentPageRaw));
        } catch (NumberFormatException ex) {
            return badRequest("Invalid currentPage value");
        }

        // Find the book read
        Optional<BookRead> bookReadOpt = bookReadRepo.findById(bookReadId);
        if (bookReadOpt.isEmpty() || !bookReadOpt.get().getUserId().equals(user.getId())) {
            return notFound("Book read not found for this user");
        }

        BookRead br = bookReadOpt.get();

        // Validate that basis is locked and is PER_PAGE_MILESTONE
        if (br.getBookEarningBasis() != RewardEarningBasis.PER_PAGE_MILESTONE) {
            return badRequest("Page progress logging only valid for PER_PAGE_MILESTONE basis");
        }

        // Validate that page count is confirmed
        if (!Boolean.TRUE.equals(br.getPageCountConfirmed())) {
            return badRequest("Page count must be confirmed before logging page progress");
        }

        Integer totalPages = br.getPageCount();
        if (totalPages == null || totalPages < 1) {
            return badRequest("Total page count must be set for this book");
        }

        // Validate that currentPage is within bounds
        if (currentPage < 0 || currentPage > totalPages) {
            return badRequest("currentPage must be between 0 and " + totalPages);
        }

        // Get the reward option for this book (must be PER_PAGE_MILESTONE basis)
        User parent = userRepo.findById(user.getParentId())
            .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Parent not found"));

        List<RewardOption> eligibleOptions = rewardOptionRepo.findByOwnerUserIdOrderByCreatedAtAsc(parent.getId())
            .stream()
            .filter(option -> Boolean.TRUE.equals(option.getActive()))
            .filter(option -> option.getEarningBasis() == RewardEarningBasis.PER_PAGE_MILESTONE)
            .filter(option -> isVisibleToChild(user, option))
            .toList();

        if (eligibleOptions.isEmpty()) {
            // No reward for page progress if no eligible option
            br.setCurrentPage(currentPage);
            bookReadRepo.save(br);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("bookReadId", br.getId());
            response.put("currentPage", currentPage);
            response.put("milestonesCompleted", 0);
            return ResponseEntity.ok(response);
        }

        // Calculate milestones completed
        Integer previousPage = br.getCurrentPage() != null ? br.getCurrentPage() : 0;
        Integer carryForward = br.getPageMilestoneCarryForward() != null ? br.getPageMilestoneCarryForward() : 0;
        Integer pageMilestoneSize = eligibleOptions.get(0).getPageMilestoneSize();

        int totalPagesReadSinceLast = currentPage - previousPage;
        int totalPagesForMilestone = carryForward + totalPagesReadSinceLast;
        int milestonesCompleted = totalPagesForMilestone / pageMilestoneSize;
        int newCarryForward = totalPagesForMilestone % pageMilestoneSize;

        // Award one reward per milestone completed
        for (int i = 0; i < milestonesCompleted; i++) {
            Reward reward = new Reward();
            reward.setType(RewardType.EARN);
            reward.setUserId(user.getId());
            reward.setRewardOptionId(eligibleOptions.get(0).getId());
            reward.setAmount(eligibleOptions.get(0).getEffectiveAmount());
            reward.setNote(eligibleOptions.get(0).getName());
            rewardRepo.save(reward);
        }

        // Update book read with new page progress and carry-forward
        br.setCurrentPage(currentPage);
        br.setPageMilestoneCarryForward(newCarryForward);
        BookRead saved = bookReadRepo.save(br);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookReadId", saved.getId());
        response.put("currentPage", saved.getCurrentPage());
        response.put("milestonesCompleted", milestonesCompleted);
        response.put("pageMilestoneCarryForward", saved.getPageMilestoneCarryForward());
        return ResponseEntity.ok(response);
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
    public BookSummaryDto.CreditsDto credits(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<Reward> rewards = rewardRepo.findByUserId(user.getId());
        int totalCents = (int) rewards.stream()
                .filter(r -> r.getType() == RewardType.EARN)
                .count() * 100;
        return new BookSummaryDto.CreditsDto(totalCents, totalCents / 100.0);
    }

    // ---- Rewards ----

    @GetMapping("/rewards/summary")
    public BookSummaryDto.RewardRollupDto getRewardsSummary(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<Reward> rewards = rewardRepo.findByUserId(user.getId());

        double earned = rewards.stream()
            .filter(r -> r.getType() == RewardType.EARN)
            .mapToDouble(Reward::getAmount)
            .sum();
        double paidOut = rewards.stream()
            .filter(r -> r.getType() == RewardType.PAYOUT)
            .mapToDouble(Reward::getAmount)
            .sum();
        double spent = rewards.stream()
            .filter(r -> r.getType() == RewardType.SPEND)
            .mapToDouble(Reward::getAmount)
            .sum();

        Map<String, UnitAccumulator> unitAcc = new LinkedHashMap<>();
        for (Reward reward : rewards) {
            UnitInfo unit = resolveUnitInfo(reward);
            UnitAccumulator acc = unitAcc.computeIfAbsent(unit.key(),
                _k -> new UnitAccumulator(unit.unitType(), unit.unitLabel()));
            if (reward.getType() == RewardType.EARN) {
            acc.totalEarned += reward.getAmount();
            } else if (reward.getType() == RewardType.PAYOUT) {
            acc.totalPaidOut += reward.getAmount();
            } else if (reward.getType() == RewardType.SPEND) {
            acc.totalSpent += reward.getAmount();
            }
        }

        List<BookSummaryDto.UnitBalanceDto> balancesByUnit = unitAcc.values().stream()
            .map(acc -> new BookSummaryDto.UnitBalanceDto(
                acc.unitType,
                acc.unitLabel,
                acc.totalEarned,
                acc.totalPaidOut,
                acc.totalSpent,
                acc.totalEarned - acc.totalPaidOut - acc.totalSpent
            ))
            .toList();

        return new BookSummaryDto.RewardRollupDto(earned, paidOut, spent, earned - paidOut - spent, balancesByUnit);
    }

    @Transactional
    @GetMapping("/reward-options")
    public BookSummaryDto.RewardOptionsResponseDto getRewardOptions(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (user.getRole() == User.UserRole.PARENT) {
            requireParent(user);
            List<RewardOption> options = rewardOptionRepo.findByOwnerUserIdOrderByCreatedAtAsc(user.getId());
            if (options.isEmpty()) {
                options = List.of(createDefaultFamilyOption(user));
            }
            return new BookSummaryDto.RewardOptionsResponseDto(
                    options.stream().map(this::rewardOptionToDto).toList(),
                    null,
                    null
            );
        }

        User child = requireChild(user);
        Optional<ChildRewardSelection> activeSelection = rewardSelectionRepo.findByChildUserIdAndActiveTrue(child.getId());
        RewardOption selected = resolveVisibleSelection(child);
        User parent = userRepo.findById(child.getParentId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Parent not found"));
        List<BookSummaryDto.RewardOptionDto> options = rewardOptionRepo.findByOwnerUserIdOrderByCreatedAtAsc(parent.getId())
                .stream()
                .filter(option -> isVisibleToChild(child, option))
                .map(this::rewardOptionToDto)
                .toList();
        return new BookSummaryDto.RewardOptionsResponseDto(
                options,
                activeSelection.map(ChildRewardSelection::getId).orElse(null),
                selected != null ? selected.getId() : null
        );
    }

    @PostMapping("/reward-options")
    public ResponseEntity<?> createRewardOption(@RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User parent = requireParent(getCurrentUser(userDetails));
        RewardOption rewardOption = parseRewardOptionPayload(body, null, parent);
        return ResponseEntity.status(201).body(rewardOptionToDto(rewardOptionRepo.save(rewardOption)));
    }

    @PutMapping("/reward-options/{rewardOptionId}")
    public ResponseEntity<?> updateRewardOption(@PathVariable UUID rewardOptionId,
                                                @RequestBody Map<String, Object> body,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User parent = requireParent(getCurrentUser(userDetails));
        RewardOption existing = rewardOptionRepo.findByIdAndOwnerUserId(rewardOptionId, parent.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Reward option not found"));
        validateDefinitionEditAllowed(existing, body);
        RewardOption rewardOption = parseRewardOptionPayload(body, existing, parent);
        if (Boolean.TRUE.equals(existing.getActive()) && Boolean.FALSE.equals(rewardOption.getActive())) {
            ensureCanDeactivateRewardOption(parent, existing);
        }
        RewardOption saved = rewardOptionRepo.save(rewardOption);
        if (!Boolean.TRUE.equals(saved.getActive())) {
            deactivateSelectionsForOption(saved.getId());
        }
        return ResponseEntity.ok(rewardOptionToDto(saved));
    }

    @DeleteMapping("/reward-options/{rewardOptionId}")
    public ResponseEntity<?> deactivateRewardOption(@PathVariable UUID rewardOptionId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        User parent = requireParent(getCurrentUser(userDetails));
        RewardOption existing = rewardOptionRepo.findByIdAndOwnerUserId(rewardOptionId, parent.getId())
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Reward option not found"));
        ensureCanDeactivateRewardOption(parent, existing);
        existing.setActive(Boolean.FALSE);
        rewardOptionRepo.save(existing);
        deactivateSelectionsForOption(existing.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reward-options/{rewardOptionId}/select")
    public ResponseEntity<?> selectRewardOption(@PathVariable UUID rewardOptionId,
                                                @AuthenticationPrincipal UserDetails userDetails) {
        User child = requireChild(getCurrentUser(userDetails));
        RewardOption option = rewardOptionRepo.findById(rewardOptionId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Reward option not found"));
        if (!isVisibleToChild(child, option)) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Reward option is not available to this child");
        }

        rewardSelectionRepo.findByChildUserIdAndActiveTrue(child.getId()).ifPresent(existing -> {
            existing.setActive(Boolean.FALSE);
            rewardSelectionRepo.save(existing);
        });

        ChildRewardSelection selection = new ChildRewardSelection();
        selection.setChildUserId(child.getId());
        selection.setRewardOptionId(option.getId());
        selection.setActive(Boolean.TRUE);
        rewardSelectionRepo.save(selection);

        return ResponseEntity.ok(Map.of(
                "activeSelectionId", selection.getId(),
                "activeSelectionOptionId", option.getId(),
                "selectedOption", rewardOptionToDto(option)
        ));
    }

    @Transactional(readOnly = true)
    @GetMapping("/rewards")
    public BookSummaryDto.RewardHistoryPageDto getRewards(
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
        List<BookSummaryDto.RewardHistoryItemDto> result = allRewards.subList(from, to)
            .stream().map(this::rewardToDto).toList();
        return new BookSummaryDto.RewardHistoryPageDto(result, total);
    }

    @PostMapping("/rewards/spend")
    public ResponseEntity<?> spendReward(@RequestParam double amount,
                                         @RequestParam(required = false) UUID rewardOptionId,
                                         @RequestParam String note,
                                         @AuthenticationPrincipal UserDetails userDetails) {
        if (amount <= 0) return badRequest("Amount must be positive");
        User user = getCurrentUser(userDetails);
        RewardOption selectedOption = resolveOptionForSettlement(user, rewardOptionId);
        Reward reward = new Reward();
        reward.setType(RewardType.SPEND);
        reward.setUserId(user.getId());
        reward.setRewardOptionId(selectedOption != null ? selectedOption.getId() : null);
        reward.setAmount(amount);
        reward.setNote(note);
        rewardRepo.save(reward);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rewards/payout")
    public ResponseEntity<?> payoutReward(@RequestParam double amount,
                                          @RequestParam(required = false) UUID rewardOptionId,
                                          @AuthenticationPrincipal UserDetails userDetails) {
        if (amount <= 0) return badRequest("Amount must be positive");
        User user = getCurrentUser(userDetails);
        RewardOption selectedOption = resolveOptionForSettlement(user, rewardOptionId);
        Reward reward = new Reward();
        reward.setType(RewardType.PAYOUT);
        reward.setUserId(user.getId());
        reward.setRewardOptionId(selectedOption != null ? selectedOption.getId() : null);
        reward.setAmount(amount);
        reward.setNote("Payout");
        rewardRepo.save(reward);
        return ResponseEntity.ok().build();
    }

    private BookSummaryDto.RewardHistoryItemDto rewardToDto(Reward reward) {
        UUID chapterReadId = null;
        LocalDateTime completionDate = null;
        BookSummaryDto.ChapterRefDto chapterDto = null;
        BookSummaryDto.BookReadRefDto bookReadDto = null;
        if (reward.getType() == RewardType.EARN && reward.getChapterRead() != null) {
            ChapterRead cr = reward.getChapterRead();
            chapterReadId = cr.getId();
            completionDate = cr.getCompletionDate();
            if (cr.getChapter() != null) {
                Chapter ch = cr.getChapter();
                chapterDto = new BookSummaryDto.ChapterRefDto(
                        ch.getId(),
                        ch.getName(),
                        ch.getChapterIndex(),
                        ch.getGoogleBookId()
                );
            }
            if (cr.getBookRead() != null) {
                BookRead br = cr.getBookRead();
                BookSummaryDto.BookRefDto bookDto = null;
                if (br.getBook() != null) {
                    Book book = br.getBook();
                    bookDto = new BookSummaryDto.BookRefDto(
                            book.getGoogleBookId(),
                            book.getTitle(),
                            book.getAuthors() != null ? book.getAuthors() : List.of()
                    );
                }
                bookReadDto = new BookSummaryDto.BookReadRefDto(
                        br.getId(),
                        br.getStartDate(),
                        br.getEndDate(),
                        br.isInProgress(),
                        bookDto
                );
            }
        }
        UnitInfo unit = resolveUnitInfo(reward);
        return new BookSummaryDto.RewardHistoryItemDto(
                reward.getId(),
                reward.getType(),
                reward.getAmount(),
                reward.getNote(),
                reward.getCreatedAt(),
            reward.getRewardOptionId(),
            reward.getRewardOption() != null ? reward.getRewardOption().getName() : null,
            reward.getRewardOption() != null ? reward.getRewardOption().getEarningBasis() : null,
                unit.unitType(),
                unit.unitLabel(),
                chapterReadId,
                completionDate,
                chapterDto,
                bookReadDto
        );
    }

    private RewardOption resolveOptionForSettlement(User user, UUID rewardOptionId) {
        if (rewardOptionId != null) {
            RewardOption option = rewardOptionRepo.findById(rewardOptionId)
                    .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Reward option not found"));
            if (user.getRole() == User.UserRole.CHILD && !isVisibleToChild(user, option)) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Reward option is not available to this child");
            }
            return option;
        }
        if (user.getRole() == User.UserRole.CHILD) {
            return resolveVisibleSelection(user);
        }
        return null;
    }

    private UnitInfo resolveUnitInfo(Reward reward) {
        RewardOption option = reward.getRewardOptionId() != null
                ? rewardOptionRepo.findById(reward.getRewardOptionId()).orElse(null)
                : null;
        if (option == null || option.getValueType() == null || option.getValueType() == RewardValueType.MONEY) {
            String currency = option != null && option.getCurrencyCode() != null && !option.getCurrencyCode().isBlank()
                    ? option.getCurrencyCode().toUpperCase(Locale.ROOT)
                    : "USD";
            return new UnitInfo("MONEY", currency, "MONEY|" + currency);
        }

        String label = option.getNonMoneyUnitLabel() != null && !option.getNonMoneyUnitLabel().isBlank()
                ? option.getNonMoneyUnitLabel().trim()
                : "units";
        return new UnitInfo("NON_MONEY", label, "NON_MONEY|" + label.toLowerCase(Locale.ROOT));
    }

    private ResponseEntity<ErrorResponse> badRequest(String message) {
        return ResponseEntity.badRequest().body(new ErrorResponse("bad_request", message, 400));
    }

    private ResponseEntity<ErrorResponse> notFound(String message) {
        return ResponseEntity.status(404).body(new ErrorResponse("not_found", message, 404));
    }

    private static final class BookAccumulator {
        private final String googleBookId;
        private final String title;
        private final String description;
        private final String thumbnailUrl;
        private final List<String> authors;
        private final boolean inProgress;
        private int readCount;
        private LocalDateTime endDate;

        private BookAccumulator(String googleBookId, String title, String description, String thumbnailUrl,
                                List<String> authors, boolean inProgress, int readCount, LocalDateTime endDate) {
            this.googleBookId = googleBookId;
            this.title = title;
            this.description = description;
            this.thumbnailUrl = thumbnailUrl;
            this.authors = authors;
            this.inProgress = inProgress;
            this.readCount = readCount;
            this.endDate = endDate;
        }
    }

    private static final class UnitAccumulator {
        private final String unitType;
        private final String unitLabel;
        private double totalEarned;
        private double totalPaidOut;
        private double totalSpent;

        private UnitAccumulator(String unitType, String unitLabel) {
            this.unitType = unitType;
            this.unitLabel = unitLabel;
        }
    }

    private record UnitInfo(String unitType, String unitLabel, String key) {}

    private record ErrorResponse(String error, String message, int status) {}
}
