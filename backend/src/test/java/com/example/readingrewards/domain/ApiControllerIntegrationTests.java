package com.example.readingrewards.domain;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.auth.service.VerificationEmailService;
import com.example.readingrewards.domain.dto.BookSummaryDto;
import com.example.readingrewards.domain.model.Book;
import com.example.readingrewards.domain.model.BookRead;
import com.example.readingrewards.domain.model.Chapter;
import com.example.readingrewards.domain.model.ChapterRead;
import com.example.readingrewards.domain.model.Reward;
import com.example.readingrewards.domain.model.RewardEarningBasis;
import com.example.readingrewards.domain.model.RewardOption;
import com.example.readingrewards.domain.model.RewardScopeType;
import com.example.readingrewards.domain.model.RewardType;
import com.example.readingrewards.domain.model.RewardValueType;
import com.example.readingrewards.domain.repo.BookReadRepository;
import com.example.readingrewards.domain.repo.BookRepository;
import com.example.readingrewards.domain.repo.ChildRewardSelectionRepository;
import com.example.readingrewards.domain.repo.ChapterReadRepository;
import com.example.readingrewards.domain.repo.ChapterRepository;
import com.example.readingrewards.domain.repo.RewardOptionRepository;
import com.example.readingrewards.domain.repo.RewardRepository;
import com.example.readingrewards.domain.service.GoogleBooksService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerIntegrationTests {

    @MockBean
    private VerificationEmailService verificationEmailService;

    @MockBean
    private GoogleBooksService googleBooksService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookReadRepository bookReadRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private ChapterReadRepository chapterReadRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private RewardOptionRepository rewardOptionRepository;

    @Autowired
    private ChildRewardSelectionRepository childRewardSelectionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwt;

    @BeforeEach
    void setUp() throws Exception {
        childRewardSelectionRepository.deleteAll();
        rewardRepository.deleteAll();
        rewardOptionRepository.deleteAll();
        chapterReadRepository.deleteAll();
        chapterRepository.deleteAll();
        bookReadRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();

        // Create a verified parent user
        User parent = new User();
        parent.setEmail("child-test@example.com");
        parent.setPassword(passwordEncoder.encode("pass123"));
        parent.setRole(User.UserRole.PARENT);
        parent.setFirstName("Test");
        parent.setStatus("VERIFIED");
        userRepository.save(parent);

        // Login to get JWT
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"child-test@example.com","password":"pass123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        jwt = (String) body.get("token");

        when(googleBooksService.search(any(), any(), any())).thenReturn(List.of());
    }

    private User getCurrentParent() {
        return userRepository.findByEmail("child-test@example.com").orElseThrow();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn();

        Map<?, ?> body = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        return body.get("token").toString();
    }

    private User createChildForCurrentParent(String username, String firstName) {
        User parent = getCurrentParent();
        User child = new User();
        child.setUsername(username);
        child.setFirstName(firstName);
        child.setPassword(passwordEncoder.encode("kidpass"));
        child.setRole(User.UserRole.CHILD);
        child.setStatus("VERIFIED");
        child.setParentId(parent.getId());
        return userRepository.save(child);
    }

    private String createChildAndLogin(String username, String firstName) throws Exception {
        createChildForCurrentParent(username, firstName);
        return loginAndGetToken(username, "kidpass");
    }

    private void ensureDefaultRewardOptionExists() throws Exception {
        mockMvc.perform(get("/api/reward-options")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());
    }

    private String seedChildReadingData(User child, String googleBookId) {
        Book book = new Book();
        book.setGoogleBookId(googleBookId);
        book.setTitle("Child Book " + googleBookId);
        book.setDescription("seeded");
        book.setAuthors(List.of("Seed Author"));
        book.setThumbnailUrl("");
        bookRepository.save(book);

        BookRead bookRead = new BookRead();
        bookRead.setGoogleBookId(googleBookId);
        bookRead.setUserId(child.getId());
        bookRead.setStartDate(LocalDateTime.now().minusDays(1));
        BookRead savedBookRead = bookReadRepository.save(bookRead);

        Chapter chapter = new Chapter();
        chapter.setGoogleBookId(googleBookId);
        chapter.setName("Chapter 1");
        chapter.setChapterIndex(0);
        Chapter savedChapter = chapterRepository.save(chapter);

        ChapterRead chapterRead = new ChapterRead();
        chapterRead.setBookReadId(savedBookRead.getId());
        chapterRead.setChapterId(savedChapter.getId());
        chapterRead.setUserId(child.getId());
        chapterRead.setCompletionDate(LocalDateTime.now());
        ChapterRead savedChapterRead = chapterReadRepository.save(chapterRead);

        Reward reward = new Reward();
        reward.setType(RewardType.EARN);
        reward.setUserId(child.getId());
        reward.setChapterReadId(savedChapterRead.getId());
        reward.setAmount(1.0);
        reward.setNote("seed reward");
        rewardRepository.save(reward);

        return savedChapterRead.getId().toString();
    }

    @Test
    void addBookCreatesBookAndBookRead() throws Exception {
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "googleBookId": "book123",
                      "title": "Test Book",
                      "authors": ["Author One"],
                      "description": "A desc",
                      "thumbnailUrl": "http://img.example.com/thumb.jpg"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.googleBookId").value("book123"))
            .andExpect(jsonPath("$.title").value("Test Book"));

        assertThat(bookRepository.findById("book123")).isPresent();
        assertThat(bookReadRepository.findAll()).hasSize(1);
        assertThat(bookReadRepository.findAll().get(0).isInProgress()).isTrue();
    }

    @Test
    void addBookDoesNotCreateDuplicateInProgressEntryForSameUser() throws Exception {
        MvcResult firstAdd = mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "googleBookId": "book123",
                      "title": "Test Book",
                      "authors": ["Author One"],
                      "description": "A desc",
                      "thumbnailUrl": "http://img.example.com/thumb.jpg"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String firstBookReadId = objectMapper.readTree(firstAdd.getResponse().getContentAsString())
            .get("id")
            .asText();

        MvcResult secondAdd = mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "googleBookId": "book123",
                      "title": "Test Book",
                      "authors": ["Author One"],
                      "description": "A desc",
                      "thumbnailUrl": "http://img.example.com/thumb.jpg"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String secondBookReadId = objectMapper.readTree(secondAdd.getResponse().getContentAsString())
            .get("id")
            .asText();

        assertThat(secondBookReadId).isEqualTo(firstBookReadId);
        assertThat(bookReadRepository.findAll()).hasSize(1);
    }

    @Test
    void searchAddFinishAndRereadWorkflowIsSupported() throws Exception {
        String childToken = createChildAndLogin("kid_search_flow", "Kid Search");

        when(googleBooksService.search(any(), any(), any())).thenReturn(List.of(
                new BookSummaryDto(
                "OL-SEARCH-1",
                        "Searchable Book",
                        List.of("Search Author"),
                        "Search description",
                        "http://img.example.com/search.jpg"
                )
        ));

        mockMvc.perform(get("/api/search")
                .param("title", "Searchable")
                .param("author", "Search Author")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].googleBookId").value("OL-SEARCH-1"))
            .andExpect(jsonPath("$[0].title").value("Searchable Book"));

        MvcResult addRes = mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                                            "googleBookId": "OL-SEARCH-1",
                      "title": "Searchable Book",
                      "authors": ["Search Author"],
                      "description": "Search description",
                      "thumbnailUrl": ""
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String googleBookId = objectMapper.readValue(addRes.getResponse().getContentAsString(), Map.class)
                .get("googleBookId").toString();

        mockMvc.perform(post("/api/books/" + googleBookId + "/finish")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll()).hasSize(1);
        assertThat(bookReadRepository.findAll().get(0).isInProgress()).isFalse();

        mockMvc.perform(post("/api/books/" + googleBookId + "/reread")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll()).hasSize(2);
        assertThat(bookReadRepository.findAll().stream().filter(BookRead::isInProgress).count()).isEqualTo(1);
    }

    @Test
    void addChaptersAndMarkReadEarnsReward() throws Exception {
        ensureDefaultRewardOptionExists();
        String childToken = createChildAndLogin("kid_add_chapters", "Kid Chapters");

        // Add book
        mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk1","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":"","earningBasis":"PER_CHAPTER"}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        // Add chapters
        mockMvc.perform(post("/api/books/gbk1/chapters")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0},{"name":"Chapter 2","chapterIndex":1}]
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk1").get(0).getId().toString();

        // Mark chapter as read
        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll().get(0).getAmount()).isEqualTo(1.0);
    }

    @Test
    void duplicateMarkReadIsIdempotentAndDoesNotCreateDuplicateReward() throws Exception {
        ensureDefaultRewardOptionExists();
        String childToken = createChildAndLogin("kid_mark_idempotent", "Kid Idempotent");

        mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-idem","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":"","earningBasis":"PER_CHAPTER"}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-idem/chapters")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-idem").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll()).hasSize(1);
    }

    @Test
    void rewardOptionCrudCreateUpdateDeactivateWorksForParent() throws Exception {
        mockMvc.perform(get("/api/reward-options")
            .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        MvcResult createResult = mockMvc.perform(post("/api/reward-options")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scopeType": "FAMILY",
                      "earningBasis": "PER_BOOK",
                      "amount": 5.0,
                      "active": true
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("$5 per book"))
            .andReturn();

        String rewardOptionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .get("id")
            .asText();

        mockMvc.perform(put("/api/reward-options/" + rewardOptionId)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scopeType": "FAMILY",
                      "earningBasis": "PER_BOOK",
                      "amount": 7.5,
                      "active": true
                    }
                    """))
            .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value("$7.5 per book"))
            .andExpect(jsonPath("$.moneyAmount").value(7.5));

        mockMvc.perform(delete("/api/reward-options/" + rewardOptionId)
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isNoContent());

        RewardOption stored = rewardOptionRepository.findById(UUID.fromString(rewardOptionId)).orElseThrow();
        assertThat(stored.getActive()).isFalse();
        assertThat(stored.getName()).isEqualTo("$7.5 per book");
        assertThat(stored.getDescription()).isNull();
    }

    @Test
    void defaultRewardCannotBeEditedAndLastActiveRewardCannotBeDeactivated() throws Exception {
        mockMvc.perform(get("/api/reward-options")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.options[0].name").value("$1 per chapter"));

        RewardOption defaultOption = rewardOptionRepository.findByOwnerUserIdOrderByCreatedAtAsc(getCurrentParent().getId()).stream()
            .filter(option -> option.getScopeType() == RewardScopeType.FAMILY)
            .findFirst()
            .orElseThrow();

        String defaultRewardOptionId = defaultOption.getId().toString();

        mockMvc.perform(put("/api/reward-options/" + defaultRewardOptionId)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": 2.0
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/reward-options/" + defaultRewardOptionId)
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/reward-options")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "scopeType": "FAMILY",
                      "earningBasis": "PER_BOOK",
                      "amount": 5.0,
                      "active": true
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/reward-options/" + defaultRewardOptionId)
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isNoContent());

        RewardOption storedDefault = rewardOptionRepository.findById(UUID.fromString(defaultRewardOptionId)).orElseThrow();
        assertThat(storedDefault.getActive()).isFalse();
    }

    @Test
    void rewardOptionValidationRejectsMissingChildIdAndMissingPageMilestoneSize() throws Exception {
        mockMvc.perform(post("/api/reward-options")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Child Only",
                      "scopeType": "CHILD",
                      "earningBasis": "PER_CHAPTER",
                      "amount": 1.0,
                      "active": true
                    }
                    """))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/reward-options")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Pages Reward",
                      "scopeType": "FAMILY",
                      "earningBasis": "PER_PAGE_MILESTONE",
                      "amount": 1.0,
                      "active": true
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void childRewardOptionsAreAdditiveFamilyPlusMatchingChildScope() throws Exception {
        User parent = getCurrentParent();
        User childA = createChildForCurrentParent("kid_scope_a", "Kid A");
        User childB = createChildForCurrentParent("kid_scope_b", "Kid B");
        String childToken = loginAndGetToken("kid_scope_a", "kidpass");

        RewardOption familyOption = new RewardOption();
        familyOption.setOwnerUserId(parent.getId());
        familyOption.setScopeType(RewardScopeType.FAMILY);
        familyOption.setName("Family chapter reward");
        familyOption.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
        familyOption.setValueType(com.example.readingrewards.domain.model.RewardValueType.MONEY);
        familyOption.setMoneyAmount(1.0);
        familyOption.setActive(Boolean.TRUE);
        rewardOptionRepository.save(familyOption);

        RewardOption childAOption = new RewardOption();
        childAOption.setOwnerUserId(parent.getId());
        childAOption.setScopeType(RewardScopeType.CHILD);
        childAOption.setChildUserId(childA.getId());
        childAOption.setName("Kid A bonus");
        childAOption.setEarningBasis(RewardEarningBasis.PER_BOOK);
        childAOption.setValueType(com.example.readingrewards.domain.model.RewardValueType.MONEY);
        childAOption.setMoneyAmount(3.0);
        childAOption.setActive(Boolean.TRUE);
        rewardOptionRepository.save(childAOption);

        RewardOption childBOption = new RewardOption();
        childBOption.setOwnerUserId(parent.getId());
        childBOption.setScopeType(RewardScopeType.CHILD);
        childBOption.setChildUserId(childB.getId());
        childBOption.setName("Kid B bonus");
        childBOption.setEarningBasis(RewardEarningBasis.PER_BOOK);
        childBOption.setValueType(com.example.readingrewards.domain.model.RewardValueType.MONEY);
        childBOption.setMoneyAmount(4.0);
        childBOption.setActive(Boolean.TRUE);
        rewardOptionRepository.save(childBOption);

        MvcResult optionsResult = mockMvc.perform(get("/api/reward-options")
                .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk())
            .andReturn();

        String content = optionsResult.getResponse().getContentAsString();
        List<String> names = objectMapper.readTree(content)
            .withArray("options")
            .findValuesAsText("name");

        assertThat(names).contains("$1 per chapter", "$3 per book");
        assertThat(names).doesNotContain("$4 per book");
    }

        @Test
        void bookBasisSelectionLocksOncePerInProgressBook() throws Exception {
                User child = createChildForCurrentParent("kid_basis_lock", "Kid Basis");
                String childToken = loginAndGetToken("kid_basis_lock", "kidpass");

                mockMvc.perform(post("/api/books")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "googleBookId": "book-basis-lock",
                                            "title": "Basis Lock Book",
                                            "authors": ["Author One"],
                                            "description": "desc",
                                            "thumbnailUrl": ""
                                        }
                                        """))
                        .andExpect(status().isOk());

                mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-basis-lock/basis-selection")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "earningBasis": "PER_CHAPTER"
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.bookEarningBasis").value("PER_CHAPTER"));

                mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-basis-lock/basis-selection")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "earningBasis": "PER_BOOK"
                                        }
                                        """))
                        .andExpect(status().isConflict());
        }

            @Test
            void bookBasisSelectionCanConfirmPageCountForPerPageMilestones() throws Exception {
                User child = createChildForCurrentParent("kid_page_basis", "Kid Page");
                String childToken = loginAndGetToken("kid_page_basis", "kidpass");

                mockMvc.perform(post("/api/books")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "googleBookId": "book-page-basis",
                                "title": "Page Basis Book",
                                "authors": ["Author One"],
                                "description": "desc",
                                "thumbnailUrl": "",
                                "pageCount": 280
                            }
                            """))
                    .andExpect(status().isOk());

                mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-page-basis/basis-selection")
                        .header("Authorization", "Bearer " + childToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "earningBasis": "PER_PAGE_MILESTONE",
                                "pageCount": 333
                            }
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookEarningBasis").value("PER_PAGE_MILESTONE"))
                    .andExpect(jsonPath("$.pageCount").value(333))
                    .andExpect(jsonPath("$.pageCountConfirmed").value(true));

                BookRead saved = bookReadRepository.findByUserIdAndGoogleBookIdAndEndDateIsNull(child.getId(), "book-page-basis").orElseThrow();
                assertThat(saved.getPageCount()).isEqualTo(333);
                assertThat(saved.getPageCountConfirmed()).isTrue();
            }

        @Test
        void chapterRewardsRequirePerChapterBasisSelectionBeforeEarning() throws Exception {
                User parent = getCurrentParent();
                User child = createChildForCurrentParent("kid_basis_gate", "Kid Gate");
                String childToken = loginAndGetToken("kid_basis_gate", "kidpass");

                RewardOption chapterOption = new RewardOption();
                chapterOption.setOwnerUserId(parent.getId());
                chapterOption.setScopeType(RewardScopeType.FAMILY);
                chapterOption.setName("Chapter Reward");
                chapterOption.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
                chapterOption.setValueType(RewardValueType.MONEY);
                chapterOption.setCurrencyCode("USD");
                chapterOption.setMoneyAmount(1.0);
                chapterOption.setActive(Boolean.TRUE);
                RewardOption savedOption = rewardOptionRepository.save(chapterOption);

                mockMvc.perform(post("/api/reward-options/" + savedOption.getId() + "/select")
                    .header("Authorization", "Bearer " + childToken))
                        .andExpect(status().isOk());

                MvcResult addBook = mockMvc.perform(post("/api/books")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "googleBookId": "book-basis-gate",
                                            "title": "Basis Gate Book",
                                            "authors": ["Author One"],
                                            "description": "desc",
                                            "thumbnailUrl": ""
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn();

                UUID bookReadId = UUID.fromString(objectMapper.readTree(addBook.getResponse().getContentAsString()).get("id").asText());

                MvcResult chapterResult = mockMvc.perform(post("/api/books/book-basis-gate/chapters")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        [
                                            {"name":"Chapter 1","chapterIndex":1},
                                            {"name":"Chapter 2","chapterIndex":2}
                                        ]
                                        """))
                        .andExpect(status().isOk())
                        .andReturn();

                UUID chapter1Id = UUID.fromString(objectMapper.readTree(chapterResult.getResponse().getContentAsString()).get(0).get("id").asText());
                UUID chapter2Id = UUID.fromString(objectMapper.readTree(chapterResult.getResponse().getContentAsString()).get(1).get("id").asText());

                mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapter1Id + "/read")
                                .header("Authorization", "Bearer " + childToken))
                        .andExpect(status().isOk());

                assertThat(rewardRepository.findByUserId(child.getId())).isEmpty();

                mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-basis-gate/basis-selection")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "earningBasis": "PER_CHAPTER"
                                        }
                                        """))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapter2Id + "/read")
                                .header("Authorization", "Bearer " + childToken))
                        .andExpect(status().isOk());

                assertThat(rewardRepository.findByUserId(child.getId())).hasSize(1);
        }

    @Test
    void creditsReflectEarnedRewards() throws Exception {
        ensureDefaultRewardOptionExists();
        String childToken = createChildAndLogin("kid_credits", "Kid Credits");

        // Add book + chapter + mark read twice
        mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk2","title":"B2","authors":["A"],"description":"","thumbnailUrl":"","earningBasis":"PER_CHAPTER"}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk2/chapters")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Ch1","chapterIndex":0},{"name":"Ch2","chapterIndex":1}]
                    """))
            .andExpect(status().isOk());

        var chapters = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk2");
        for (var ch : chapters) {
            mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + ch.getId() + "/read")
                    .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/credits")
                .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cents").value(200))
            .andExpect(jsonPath("$.dollars").value(2.0));
    }

    @Test
    void rewardsSummaryReflectsBalance() throws Exception {
        mockMvc.perform(get("/api/rewards/summary")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEarned").value(0.0))
            .andExpect(jsonPath("$.currentBalance").value(0.0));
    }

    @Test
    void payoutReducesCurrentBalanceInSummary() throws Exception {
        ensureDefaultRewardOptionExists();
        String childToken = createChildAndLogin("kid_payout_summary", "Kid Payout");

        mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-pay","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":"","earningBasis":"PER_CHAPTER"}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-pay/chapters")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-pay").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/rewards/payout")
                .header("Authorization", "Bearer " + childToken)
                .param("amount", "0.50"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rewards/summary")
                .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEarned").value(1.0))
            .andExpect(jsonPath("$.totalPaidOut").value(0.5))
            .andExpect(jsonPath("$.currentBalance").value(0.5));
    }

    @Test
    void parentCanAddAndListKids() throws Exception {
        mockMvc.perform(post("/api/parent/kids")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"kiduser","firstName":"Kid","password":"kidpass"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/parent/kids")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("kiduser"));
    }

    @Test
    void parentKidsSummaryReturnsDashboardCardPayload() throws Exception {
        User kid = createChildForCurrentParent("kid-summary", "Taylor");
        seedChildReadingData(kid, "gbk-summary-card");

        mockMvc.perform(post("/api/rewards/spend")
                .header("Authorization", "Bearer " + loginAndGetToken("kid-summary", "kidpass"))
                .param("amount", "0.25")
                .param("note", "Sticker"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/parent/kids/summary")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.kids.length()").value(1))
            .andExpect(jsonPath("$.kids[0].id").value(kid.getId().toString()))
            .andExpect(jsonPath("$.kids[0].firstName").value("Taylor"))
            .andExpect(jsonPath("$.kids[0].username").value("kid-summary"))
            .andExpect(jsonPath("$.kids[0].booksRead").value(1))
            .andExpect(jsonPath("$.kids[0].chaptersRead").value(1))
            .andExpect(jsonPath("$.kids[0].totalEarned").value(1.0))
            .andExpect(jsonPath("$.kids[0].currentBalance").value(0.75));
    }

    @Test
    void parentChildDetailReturnsPayloadAndEnforcesBoundaries() throws Exception {
        User child = createChildForCurrentParent("kid-detail", "Jamie");
        seedChildReadingData(child, "gbk-parent-detail");

        mockMvc.perform(get("/api/parent/" + child.getId() + "/child-detail")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.child.id").value(child.getId().toString()))
            .andExpect(jsonPath("$.child.username").value("kid-detail"))
            .andExpect(jsonPath("$.books.length()").value(1))
            .andExpect(jsonPath("$.books[0].chapters.length()").value(1))
            .andExpect(jsonPath("$.books[0].chapters[0].isRead").value(true))
            .andExpect(jsonPath("$.rewards.length()").value(1))
            .andExpect(jsonPath("$.totalEarned").value(1.0));

        User otherParent = new User();
        otherParent.setEmail("other-parent@example.com");
        otherParent.setPassword(passwordEncoder.encode("pass123"));
        otherParent.setRole(User.UserRole.PARENT);
        otherParent.setFirstName("Other");
        otherParent.setStatus("VERIFIED");
        userRepository.save(otherParent);
        String otherParentJwt = loginAndGetToken("other-parent@example.com", "pass123");

        mockMvc.perform(get("/api/parent/" + child.getId() + "/child-detail")
                .header("Authorization", "Bearer " + otherParentJwt))
            .andExpect(status().isNotFound());

        String childJwt = loginAndGetToken("kid-detail", "kidpass");
        mockMvc.perform(get("/api/parent/" + child.getId() + "/child-detail")
                .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isForbidden());
    }

    @Test
    void parentReverseChapterReadRemovesChapterReadAndRewardAndEnforcesOwnership() throws Exception {
        User child = createChildForCurrentParent("kid-reverse", "Riley");
        String chapterReadId = seedChildReadingData(child, "gbk-parent-reverse");

        assertThat(chapterReadRepository.findByUserId(child.getId())).hasSize(1);
        assertThat(rewardRepository.findByUserId(child.getId())).hasSize(1);

        mockMvc.perform(post("/api/parent/" + child.getId() + "/chapter-reads/" + chapterReadId + "/reverse")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findByUserId(child.getId())).isEmpty();
        assertThat(rewardRepository.findByUserId(child.getId())).isEmpty();

        String secondChapterReadId = seedChildReadingData(child, "gbk-parent-reverse-2");
        User otherParent = new User();
        otherParent.setEmail("not-owner@example.com");
        otherParent.setPassword(passwordEncoder.encode("pass123"));
        otherParent.setRole(User.UserRole.PARENT);
        otherParent.setFirstName("NotOwner");
        otherParent.setStatus("VERIFIED");
        userRepository.save(otherParent);
        String otherParentJwt = loginAndGetToken("not-owner@example.com", "pass123");

        mockMvc.perform(post("/api/parent/" + child.getId() + "/chapter-reads/" + secondChapterReadId + "/reverse")
                .header("Authorization", "Bearer " + otherParentJwt))
            .andExpect(status().isNotFound());

        assertThat(chapterReadRepository.findByUserId(child.getId())).hasSize(1);
        assertThat(rewardRepository.findByUserId(child.getId())).hasSize(1);
    }

        @Test
        void chapterCompletionBlocksWhenMultipleEligibleOptionsAndNoExplicitChoice() throws Exception {
                User parent = getCurrentParent();
                User child = createChildForCurrentParent("kid_multi_options", "Kid Multi");
                String childToken = loginAndGetToken("kid_multi_options", "kidpass");

                RewardOption optionA = new RewardOption();
                optionA.setOwnerUserId(parent.getId());
                optionA.setScopeType(RewardScopeType.FAMILY);
                optionA.setName("Option A");
                optionA.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
                optionA.setValueType(RewardValueType.MONEY);
                optionA.setCurrencyCode("USD");
                optionA.setMoneyAmount(1.0);
                optionA.setActive(Boolean.TRUE);
                rewardOptionRepository.save(optionA);

                RewardOption optionB = new RewardOption();
                optionB.setOwnerUserId(parent.getId());
                optionB.setScopeType(RewardScopeType.FAMILY);
                optionB.setName("Option B");
                optionB.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
                optionB.setValueType(RewardValueType.MONEY);
                optionB.setCurrencyCode("USD");
                optionB.setMoneyAmount(2.0);
                optionB.setActive(Boolean.TRUE);
                rewardOptionRepository.save(optionB);

                MvcResult addBook = mockMvc.perform(post("/api/books")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "googleBookId": "book-multi-options",
                                            "title": "Multi Option Book",
                                            "authors": ["Author One"],
                                            "description": "desc",
                                            "thumbnailUrl": ""
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn();

                UUID bookReadId = UUID.fromString(objectMapper.readTree(addBook.getResponse().getContentAsString()).get("id").asText());

                MvcResult chapterResult = mockMvc.perform(post("/api/books/book-multi-options/chapters")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        [
                                            {"name":"Chapter 1","chapterIndex":1}
                                        ]
                                        """))
                        .andExpect(status().isOk())
                        .andReturn();

                UUID chapter1Id = UUID.fromString(objectMapper.readTree(chapterResult.getResponse().getContentAsString()).get(0).get("id").asText());

                mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-multi-options/basis-selection")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "earningBasis": "PER_CHAPTER"
                                        }
                                        """))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapter1Id + "/read")
                                .header("Authorization", "Bearer " + childToken))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.availableOptions").isArray())
                        .andExpect(jsonPath("$.availableOptions.length()").value(2));

                assertThat(rewardRepository.findByUserId(child.getId())).isEmpty();
        }

        @Test
        void chapterCompletionUsesExplicitSelectedOptionWhenProvided() throws Exception {
                User parent = getCurrentParent();
                User child = createChildForCurrentParent("kid_explicit_option", "Kid Explicit");
                String childToken = loginAndGetToken("kid_explicit_option", "kidpass");

                RewardOption optionA = new RewardOption();
                optionA.setOwnerUserId(parent.getId());
                optionA.setScopeType(RewardScopeType.FAMILY);
                optionA.setName("Option A");
                optionA.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
                optionA.setValueType(RewardValueType.MONEY);
                optionA.setCurrencyCode("USD");
                optionA.setMoneyAmount(1.0);
                optionA.setActive(Boolean.TRUE);
                rewardOptionRepository.save(optionA);

                RewardOption optionB = new RewardOption();
                optionB.setOwnerUserId(parent.getId());
                optionB.setScopeType(RewardScopeType.FAMILY);
                optionB.setName("Option B");
                optionB.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
                optionB.setValueType(RewardValueType.MONEY);
                optionB.setCurrencyCode("USD");
                optionB.setMoneyAmount(2.0);
                optionB.setActive(Boolean.TRUE);
                RewardOption savedB = rewardOptionRepository.save(optionB);

                MvcResult addBook = mockMvc.perform(post("/api/books")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "googleBookId": "book-explicit-option",
                                            "title": "Explicit Option Book",
                                            "authors": ["Author One"],
                                            "description": "desc",
                                            "thumbnailUrl": ""
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andReturn();

                UUID bookReadId = UUID.fromString(objectMapper.readTree(addBook.getResponse().getContentAsString()).get("id").asText());

                MvcResult chapterResult = mockMvc.perform(post("/api/books/book-explicit-option/chapters")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        [
                                            {"name":"Chapter 1","chapterIndex":1}
                                        ]
                                        """))
                        .andExpect(status().isOk())
                        .andReturn();

                UUID chapter1Id = UUID.fromString(objectMapper.readTree(chapterResult.getResponse().getContentAsString()).get(0).get("id").asText());

                mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-explicit-option/basis-selection")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "earningBasis": "PER_CHAPTER"
                                        }
                                        """))
                        .andExpect(status().isOk());

                mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapter1Id + "/read")
                                .header("Authorization", "Bearer " + childToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "rewardOptionId": "%s"
                                        }
                                        """.formatted(savedB.getId())))
                        .andExpect(status().isOk());

                List<Reward> rewards = rewardRepository.findByUserId(child.getId());
                assertThat(rewards).hasSize(1);
                assertThat(rewards.get(0).getRewardOptionId()).isEqualTo(savedB.getId());
                assertThat(rewards.get(0).getAmount()).isEqualTo(2.0);
        }

    @Test
    void finishBookSetsEndDateAndRereadCreatesNewBookRead() throws Exception {
        createChildForCurrentParent("kid_finish_flow", "Kid Finish");
        String childJwt = loginAndGetToken("kid_finish_flow", "kidpass");

        // Add book
        mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-finish","title":"Finish Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll().get(0).isInProgress()).isTrue();

        // Finish book
        mockMvc.perform(post("/api/books/gbk-finish/finish")
            .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll().get(0).isInProgress()).isFalse();

        // Reread creates a new BookRead
        mockMvc.perform(post("/api/books/gbk-finish/reread")
            .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll()).hasSize(2);
        long inProgressCount = bookReadRepository.findAll().stream().filter(br -> br.isInProgress()).count();
        assertThat(inProgressCount).isEqualTo(1);
    }

    @Test
    void deleteChapterReadReversesReward() throws Exception {
        ensureDefaultRewardOptionExists();
        String childToken = createChildAndLogin("kid_delete_chapter_read", "Kid Delete Chapter");

        // Add book + chapter + mark read
        mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-del-cr","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":"","earningBasis":"PER_CHAPTER"}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-del-cr/chapters")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-del-cr").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(rewardRepository.findAll()).hasSize(1);
        assertThat(chapterReadRepository.findAll()).hasSize(1);

        // Delete chapter read — should also delete reward
        mockMvc.perform(delete("/api/books/gbk-del-cr/chapters/" + chapterId + "/read")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).isEmpty();
        assertThat(rewardRepository.findAll()).isEmpty();
    }

    @Test
    void deleteBookReadCascadesChapterReadsAndRewards() throws Exception {
        ensureDefaultRewardOptionExists();
        String childToken = createChildAndLogin("kid_delete_book_read", "Kid Delete BookRead");

        // Add book + chapter + mark read
        mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-del-br","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":"","earningBasis":"PER_CHAPTER"}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-del-br/chapters")
            .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Ch1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-del-br").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll()).hasSize(1);

        // Delete the BookRead — should cascade-delete ChapterRead + Reward
        mockMvc.perform(delete("/api/bookreads/" + bookReadId)
            .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll()).isEmpty();
        assertThat(chapterReadRepository.findAll()).isEmpty();
        assertThat(rewardRepository.findAll()).isEmpty();
    }

    @Test
    void explicitChapterRenamePersistsUpdatedName() throws Exception {
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-rename","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/books/gbk-rename/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Original Name","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-rename")
                .get(0)
                .getId()
                .toString();

        mockMvc.perform(put("/api/chapters/" + chapterId)
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Renamed Chapter"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed Chapter"));

        mockMvc.perform(get("/api/books/gbk-rename/chapters")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Renamed Chapter"));
    }

        @Test
        void chaptersSeededByFirstReaderAreReusedBySecondReader() throws Exception {
        MvcResult addFirst = mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"googleBookId":"OL82563W","title":"Shared Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                """))
            .andExpect(status().isOk())
            .andReturn();

        String firstBookReadId = objectMapper.readValue(addFirst.getResponse().getContentAsString(), Map.class)
            .get("id").toString();

        mockMvc.perform(post("/api/bookreads/" + firstBookReadId + "/chapters")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                [{"name":"Chapter 1","chapterIndex":1},{"name":"Chapter 2","chapterIndex":2},{"name":"Chapter 3","chapterIndex":3}]
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));

        User secondReader = new User();
        secondReader.setEmail("second-reader@example.com");
        secondReader.setPassword(passwordEncoder.encode("pass123"));
        secondReader.setRole(User.UserRole.PARENT);
        secondReader.setFirstName("Second");
        secondReader.setStatus("VERIFIED");
        userRepository.save(secondReader);

        MvcResult secondLogin = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"second-reader@example.com","password":"pass123"}
                """))
            .andExpect(status().isOk())
            .andReturn();

        String secondJwt = objectMapper.readValue(secondLogin.getResponse().getContentAsString(), Map.class)
            .get("token").toString();

        MvcResult addSecond = mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + secondJwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"googleBookId":"OL82563W","title":"Shared Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                """))
            .andExpect(status().isOk())
            .andReturn();

        String secondBookReadId = objectMapper.readValue(addSecond.getResponse().getContentAsString(), Map.class)
            .get("id").toString();

        mockMvc.perform(get("/api/bookreads/" + secondBookReadId + "/chapters")
            .header("Authorization", "Bearer " + secondJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].name").value("Chapter 1"))
            .andExpect(jsonPath("$[1].name").value("Chapter 2"))
            .andExpect(jsonPath("$[2].name").value("Chapter 3"));
        }

    @Test
    void spendReducesCurrentBalanceInSummary() throws Exception {
        ensureDefaultRewardOptionExists();
        String childToken = createChildAndLogin("kid_spend_summary", "Kid Spend");

        // Earn $1 by reading a chapter
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-spend","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":"","earningBasis":"PER_CHAPTER"}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-spend/chapters")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-spend").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk());

        // Spend $0.50
        mockMvc.perform(post("/api/rewards/spend")
                .header("Authorization", "Bearer " + childToken)
                .param("amount", "0.50")
                .param("note", "ice cream"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rewards/summary")
                .header("Authorization", "Bearer " + childToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEarned").value(1.0))
            .andExpect(jsonPath("$.totalSpent").value(0.5))
            .andExpect(jsonPath("$.currentBalance").value(0.5));
    }

    @Test
    void parentSelfReadingListAddListAndChildBoundaryDenial() throws Exception {
        MvcResult parentAdd = mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "googleBookId": "parent-self-gbk",
                      "title": "Parent Self Book",
                      "authors": ["Parent Author"],
                      "description": "self",
                      "thumbnailUrl": ""
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String parentBookReadId = objectMapper.readValue(parentAdd.getResponse().getContentAsString(), Map.class)
                .get("id")
                .toString();

        mockMvc.perform(get("/api/bookreads/in-progress")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Parent Self Book"));

        User child = createChildForCurrentParent("kid-boundary", "Boundary");
        String childJwt = loginAndGetToken("kid-boundary", "kidpass");

        mockMvc.perform(get("/api/bookreads/in-progress")
                .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(delete("/api/bookreads/" + parentBookReadId)
                .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isNotFound());

        assertThat(bookReadRepository.findAll()).hasSize(1);
        assertThat(bookReadRepository.findAll().get(0).getUserId()).isEqualTo(getCurrentParent().getId());
        assertThat(child.getParentId()).isEqualTo(getCurrentParent().getId());
    }


    @Test
    void rewardsSummarySeparatesBalancesByUnitType() throws Exception {
        User parent = getCurrentParent();
        User child = createChildForCurrentParent("kid_unit_sep", "Kid Unit");
        String childJwt = loginAndGetToken("kid_unit_sep", "kidpass");

        RewardOption moneyOption = new RewardOption();
        moneyOption.setOwnerUserId(parent.getId());
        moneyOption.setScopeType(RewardScopeType.FAMILY);
        moneyOption.setName("Dollar Reward");
        moneyOption.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
        moneyOption.setValueType(RewardValueType.MONEY);
        moneyOption.setCurrencyCode("USD");
        moneyOption.setMoneyAmount(1.0);
        moneyOption.setActive(Boolean.TRUE);
        RewardOption savedMoneyOption = rewardOptionRepository.save(moneyOption);

        RewardOption nonMoneyOption = new RewardOption();
        nonMoneyOption.setOwnerUserId(parent.getId());
        nonMoneyOption.setScopeType(RewardScopeType.FAMILY);
        nonMoneyOption.setName("Screen Time Reward");
        nonMoneyOption.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
        nonMoneyOption.setValueType(RewardValueType.NON_MONEY);
        nonMoneyOption.setNonMoneyQuantity(30.0);
        nonMoneyOption.setNonMoneyUnitLabel("minutes screen time");
        nonMoneyOption.setActive(Boolean.TRUE);
        RewardOption savedNonMoneyOption = rewardOptionRepository.save(nonMoneyOption);

        Reward earnMoney = new Reward();
        earnMoney.setType(RewardType.EARN);
        earnMoney.setUserId(child.getId());
        earnMoney.setRewardOptionId(savedMoneyOption.getId());
        earnMoney.setAmount(2.0);
        earnMoney.setNote("Money earn");
        rewardRepository.save(earnMoney);

        Reward earnNonMoney = new Reward();
        earnNonMoney.setType(RewardType.EARN);
        earnNonMoney.setUserId(child.getId());
        earnNonMoney.setRewardOptionId(savedNonMoneyOption.getId());
        earnNonMoney.setAmount(60.0);
        earnNonMoney.setNote("Time earn");
        rewardRepository.save(earnNonMoney);

        Reward spendMoney = new Reward();
        spendMoney.setType(RewardType.SPEND);
        spendMoney.setUserId(child.getId());
        spendMoney.setRewardOptionId(savedMoneyOption.getId());
        spendMoney.setAmount(0.5);
        spendMoney.setNote("Money spend");
        rewardRepository.save(spendMoney);

        mockMvc.perform(get("/api/rewards/summary")
                .header("Authorization", "Bearer " + childJwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.balancesByUnit").isArray())
            .andExpect(jsonPath("$.balancesByUnit.length()").value(2))
            .andExpect(jsonPath("$.balancesByUnit[?(@.unitType == 'MONEY' && @.unitLabel == 'USD')].currentBalance").value(org.hamcrest.Matchers.contains(1.5)))
            .andExpect(jsonPath("$.balancesByUnit[?(@.unitType == 'NON_MONEY' && @.unitLabel == 'minutes screen time')].currentBalance").value(org.hamcrest.Matchers.contains(60.0)));
    }

    @Test
    void spendAndPayoutCanBeAttributedToSelectedRewardUnit() throws Exception {
        User parent = getCurrentParent();
        User child = createChildForCurrentParent("kid_spend_unit", "Kid Spend Unit");
        String childJwt = loginAndGetToken("kid_spend_unit", "kidpass");

        RewardOption moneyOption = new RewardOption();
        moneyOption.setOwnerUserId(parent.getId());
        moneyOption.setScopeType(RewardScopeType.FAMILY);
        moneyOption.setName("Dollar Reward");
        moneyOption.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
        moneyOption.setValueType(RewardValueType.MONEY);
        moneyOption.setCurrencyCode("USD");
        moneyOption.setMoneyAmount(1.0);
        moneyOption.setActive(Boolean.TRUE);
        RewardOption savedMoneyOption = rewardOptionRepository.save(moneyOption);

        mockMvc.perform(post("/api/rewards/spend")
                .header("Authorization", "Bearer " + childJwt)
                .param("amount", "0.25")
                .param("note", "snack")
                .param("rewardOptionId", savedMoneyOption.getId().toString()))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/rewards/payout")
                .header("Authorization", "Bearer " + childJwt)
                .param("amount", "0.50")
                .param("rewardOptionId", savedMoneyOption.getId().toString()))
            .andExpect(status().isOk());

        List<Reward> settlementEntries = rewardRepository.findByUserId(child.getId()).stream()
                .filter(r -> r.getType() == RewardType.SPEND || r.getType() == RewardType.PAYOUT)
                .toList();

        assertThat(settlementEntries).hasSize(2);
        assertThat(settlementEntries)
                .extracting(Reward::getRewardOptionId)
                .containsOnly(savedMoneyOption.getId());
    }

    @Test
    void pageProgressLoggingCalculatesMilestonesWithCarryForward() throws Exception {
        User parent = getCurrentParent();
        User child = createChildForCurrentParent("kid_page_progress", "Kid Pages");
        String childToken = loginAndGetToken("kid_page_progress", "kidpass");

        // Create a per-page-milestone reward option (50 pages per milestone)
        RewardOption pageOption = new RewardOption();
        pageOption.setOwnerUserId(parent.getId());
        pageOption.setScopeType(RewardScopeType.FAMILY);
        pageOption.setName("50 Pages = $1");
        pageOption.setEarningBasis(RewardEarningBasis.PER_PAGE_MILESTONE);
        pageOption.setPageMilestoneSize(50);
        pageOption.setValueType(RewardValueType.MONEY);
        pageOption.setCurrencyCode("USD");
        pageOption.setMoneyAmount(1.0);
        pageOption.setActive(Boolean.TRUE);
        RewardOption savedOption = rewardOptionRepository.save(pageOption);

        // Create a book with page tracking
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "googleBookId": "book-page-progress",
                        "title": "Page Progress Book",
                        "authors": ["Author One"],
                        "description": "desc",
                        "thumbnailUrl": "",
                        "pageCount": 200
                    }
                    """))
            .andExpect(status().isOk());

        // Select PER_PAGE_MILESTONE basis and confirm page count
        MvcResult selectBasisResult = mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-page-progress/basis-selection")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "earningBasis": "PER_PAGE_MILESTONE",
                        "pageCount": 200
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String content = selectBasisResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = mapper.readValue(content, Map.class);
        UUID bookReadId = UUID.fromString((String) response.get("bookReadId"));

        // Log page progress: read 75 pages (1 milestone completed, 25 pages carry-forward)
        MvcResult pageResult1 = mockMvc.perform(post("/api/bookreads/" + bookReadId + "/pages")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPage": 75
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String pageContent1 = pageResult1.getResponse().getContentAsString();
        Map<String, Object> pageResponse1 = mapper.readValue(pageContent1, Map.class);
        assertThat(pageResponse1.get("milestonesCompleted")).isEqualTo(1);
        assertThat(pageResponse1.get("pageMilestoneCarryForward")).isEqualTo(25);

        // Verify 1 reward was earned
        List<Reward> rewards1 = rewardRepository.findByUserId(child.getId());
        assertThat(rewards1).hasSize(1);
        assertThat(rewards1.get(0).getAmount()).isEqualTo(1.0);

        // Log more pages: read 60 more pages (total 135, 1 more milestone, 35 carry-forward)
        MvcResult pageResult2 = mockMvc.perform(post("/api/bookreads/" + bookReadId + "/pages")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPage": 135
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String pageContent2 = pageResult2.getResponse().getContentAsString();
        Map<String, Object> pageResponse2 = mapper.readValue(pageContent2, Map.class);
        assertThat(pageResponse2.get("milestonesCompleted")).isEqualTo(1);
        assertThat(pageResponse2.get("pageMilestoneCarryForward")).isEqualTo(35);

        // Verify 2 rewards total now earned
        List<Reward> rewards2 = rewardRepository.findByUserId(child.getId());
        assertThat(rewards2).hasSize(2);
    }

    @Test
    void pageProgressLoggingRequiresConfirmedPageCount() throws Exception {
        User child = createChildForCurrentParent("kid_no_confirm", "Kid No Confirm");
        String childToken = loginAndGetToken("kid_no_confirm", "kidpass");

        // Create a book without confirming page count
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "googleBookId": "book-no-confirm",
                        "title": "No Confirm Book",
                        "authors": ["Author One"],
                        "description": "desc",
                        "thumbnailUrl": "",
                        "pageCount": 200
                    }
                    """))
            .andExpect(status().isOk());

        // Select PER_PAGE_MILESTONE basis but don't confirm page count
        MvcResult selectBasisResult = mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-no-confirm/basis-selection")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "earningBasis": "PER_PAGE_MILESTONE",
                        "suggestedPageCount": 200
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String content = selectBasisResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = mapper.readValue(content, Map.class);
        UUID bookReadId = UUID.fromString((String) response.get("bookReadId"));

        // Attempt to log page progress without confirming page count - should fail
        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/pages")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPage": 50
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Page count must be confirmed")));
    }

    @Test
    void pageProgressLoggingRequiresPerPageBasis() throws Exception {
        User parent = getCurrentParent();
        User child = createChildForCurrentParent("kid_chapter_basis", "Kid Chapter");
        String childToken = loginAndGetToken("kid_chapter_basis", "kidpass");

        // Create a chapter reward and select it
        RewardOption chapterOption = new RewardOption();
        chapterOption.setOwnerUserId(parent.getId());
        chapterOption.setScopeType(RewardScopeType.FAMILY);
        chapterOption.setName("Chapter Reward");
        chapterOption.setEarningBasis(RewardEarningBasis.PER_CHAPTER);
        chapterOption.setValueType(RewardValueType.MONEY);
        chapterOption.setCurrencyCode("USD");
        chapterOption.setMoneyAmount(1.0);
        chapterOption.setActive(Boolean.TRUE);
        RewardOption savedOption = rewardOptionRepository.save(chapterOption);

        mockMvc.perform(post("/api/reward-options/" + savedOption.getId() + "/select")
            .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk());

        // Create a book
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "googleBookId": "book-chapter-basis",
                        "title": "Chapter Basis Book",
                        "authors": ["Author One"],
                        "description": "desc",
                        "thumbnailUrl": ""
                    }
                    """))
            .andExpect(status().isOk());

        // Select PER_CHAPTER basis
        MvcResult selectBasisResult = mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-chapter-basis/basis-selection")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "earningBasis": "PER_CHAPTER"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String content = selectBasisResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = mapper.readValue(content, Map.class);
        UUID bookReadId = UUID.fromString((String) response.get("bookReadId"));

        // Attempt to log page progress with PER_CHAPTER basis - should fail
        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/pages")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPage": 50
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Page progress logging only valid for PER_PAGE_MILESTONE")));
    }

    @Test
    void pageProgressLoggingHandlesMultipleMilestonesInOneEvent() throws Exception {
        User parent = getCurrentParent();
        User child = createChildForCurrentParent("kid_multi_milestone", "Kid Multi");
        String childToken = loginAndGetToken("kid_multi_milestone", "kidpass");

        // Create a per-page-milestone reward option (25 pages per milestone)
        RewardOption pageOption = new RewardOption();
        pageOption.setOwnerUserId(parent.getId());
        pageOption.setScopeType(RewardScopeType.FAMILY);
        pageOption.setName("25 Pages = $2");
        pageOption.setEarningBasis(RewardEarningBasis.PER_PAGE_MILESTONE);
        pageOption.setPageMilestoneSize(25);
        pageOption.setValueType(RewardValueType.MONEY);
        pageOption.setCurrencyCode("USD");
        pageOption.setMoneyAmount(2.0);
        pageOption.setActive(Boolean.TRUE);
        RewardOption savedOption = rewardOptionRepository.save(pageOption);

        // Create a book
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "googleBookId": "book-multi-milestone",
                        "title": "Multi Milestone Book",
                        "authors": ["Author One"],
                        "description": "desc",
                        "thumbnailUrl": "",
                        "pageCount": 500
                    }
                    """))
            .andExpect(status().isOk());

        // Select PER_PAGE_MILESTONE basis
        MvcResult selectBasisResult = mockMvc.perform(put("/api/children/" + child.getId() + "/books/book-multi-milestone/basis-selection")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "earningBasis": "PER_PAGE_MILESTONE",
                        "pageCount": 500
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String content = selectBasisResult.getResponse().getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> response = mapper.readValue(content, Map.class);
        UUID bookReadId = UUID.fromString((String) response.get("bookReadId"));

        // Log page progress: read 103 pages (4 milestones: 25+25+25+25=100, with 3 carry-forward)
        MvcResult pageResult = mockMvc.perform(post("/api/bookreads/" + bookReadId + "/pages")
                .header("Authorization", "Bearer " + childToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "currentPage": 103
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String pageContent = pageResult.getResponse().getContentAsString();
        Map<String, Object> pageResponse = mapper.readValue(pageContent, Map.class);
        assertThat(pageResponse.get("milestonesCompleted")).isEqualTo(4);
        assertThat(pageResponse.get("pageMilestoneCarryForward")).isEqualTo(3);

        // Verify 4 rewards were earned ($2 each)
        List<Reward> rewards = rewardRepository.findByUserId(child.getId());
        assertThat(rewards).hasSize(4);
        assertThat(rewards).allMatch(r -> r.getAmount().equals(2.0));
    }
}
