package com.example.readingrewards.domain;

import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import com.example.readingrewards.auth.service.VerificationEmailService;
import com.example.readingrewards.domain.model.Book;
import com.example.readingrewards.domain.model.BookRead;
import com.example.readingrewards.domain.model.Chapter;
import com.example.readingrewards.domain.model.ChapterRead;
import com.example.readingrewards.domain.model.Reward;
import com.example.readingrewards.domain.model.RewardType;
import com.example.readingrewards.domain.repo.BookReadRepository;
import com.example.readingrewards.domain.repo.BookRepository;
import com.example.readingrewards.domain.repo.ChapterReadRepository;
import com.example.readingrewards.domain.repo.ChapterRepository;
import com.example.readingrewards.domain.repo.RewardRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerIntegrationTests {

    @MockBean
    private VerificationEmailService verificationEmailService;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwt;

    @BeforeEach
    void setUp() throws Exception {
        rewardRepository.deleteAll();
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
    void addChaptersAndMarkReadEarnsReward() throws Exception {
        // Add book
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk1","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        // Add chapters
        mockMvc.perform(post("/api/books/gbk1/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0},{"name":"Chapter 2","chapterIndex":1}]
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk1").get(0).getId().toString();

        // Mark chapter as read
        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll().get(0).getAmount()).isEqualTo(1.0);
    }

    @Test
    void duplicateMarkReadIsIdempotentAndDoesNotCreateDuplicateReward() throws Exception {
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-idem","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-idem/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-idem").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll()).hasSize(1);
    }

    @Test
    void creditsReflectEarnedRewards() throws Exception {
        // Add book + chapter + mark read twice
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk2","title":"B2","authors":["A"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk2/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Ch1","chapterIndex":0},{"name":"Ch2","chapterIndex":1}]
                    """))
            .andExpect(status().isOk());

        var chapters = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk2");
        for (var ch : chapters) {
            mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + ch.getId() + "/read")
                    .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/credits")
                .header("Authorization", "Bearer " + jwt))
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
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-pay","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-pay/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-pay").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/rewards/payout")
                .header("Authorization", "Bearer " + jwt)
                .param("amount", "0.50"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rewards/summary")
                .header("Authorization", "Bearer " + jwt))
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
    void finishBookSetsEndDateAndRereadCreatesNewBookRead() throws Exception {
        // Add book
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-finish","title":"Finish Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll().get(0).isInProgress()).isTrue();

        // Finish book
        mockMvc.perform(post("/api/books/gbk-finish/finish")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll().get(0).isInProgress()).isFalse();

        // Reread creates a new BookRead
        mockMvc.perform(post("/api/books/gbk-finish/reread")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll()).hasSize(2);
        long inProgressCount = bookReadRepository.findAll().stream().filter(br -> br.isInProgress()).count();
        assertThat(inProgressCount).isEqualTo(1);
    }

    @Test
    void deleteChapterReadReversesReward() throws Exception {
        // Add book + chapter + mark read
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-del-cr","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-del-cr/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-del-cr").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(rewardRepository.findAll()).hasSize(1);
        assertThat(chapterReadRepository.findAll()).hasSize(1);

        // Delete chapter read — should also delete reward
        mockMvc.perform(delete("/api/books/gbk-del-cr/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).isEmpty();
        assertThat(rewardRepository.findAll()).isEmpty();
    }

    @Test
    void deleteBookReadCascadesChapterReadsAndRewards() throws Exception {
        // Add book + chapter + mark read
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-del-br","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-del-br/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Ch1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-del-br").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(chapterReadRepository.findAll()).hasSize(1);
        assertThat(rewardRepository.findAll()).hasSize(1);

        // Delete the BookRead — should cascade-delete ChapterRead + Reward
        mockMvc.perform(delete("/api/bookreads/" + bookReadId)
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        assertThat(bookReadRepository.findAll()).isEmpty();
        assertThat(chapterReadRepository.findAll()).isEmpty();
        assertThat(rewardRepository.findAll()).isEmpty();
    }

        @Test
        void chaptersSeededByFirstReaderAreReusedBySecondReader() throws Exception {
        MvcResult addFirst = mockMvc.perform(post("/api/books")
            .header("Authorization", "Bearer " + jwt)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"googleBookId":"works/OL82563W","title":"Shared Book","authors":["Auth"],"description":"","thumbnailUrl":""}
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
                {"googleBookId":"works/OL82563W","title":"Shared Book","authors":["Auth"],"description":"","thumbnailUrl":""}
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
        // Earn $1 by reading a chapter
        mockMvc.perform(post("/api/books")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"googleBookId":"gbk-spend","title":"Book","authors":["Auth"],"description":"","thumbnailUrl":""}
                    """))
            .andExpect(status().isOk());

        String bookReadId = bookReadRepository.findAll().get(0).getId().toString();

        mockMvc.perform(post("/api/books/gbk-spend/chapters")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [{"name":"Chapter 1","chapterIndex":0}]
                    """))
            .andExpect(status().isOk());

        String chapterId = chapterRepository.findByGoogleBookIdOrderByChapterIndex("gbk-spend").get(0).getId().toString();

        mockMvc.perform(post("/api/bookreads/" + bookReadId + "/chapters/" + chapterId + "/read")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk());

        // Spend $0.50
        mockMvc.perform(post("/api/rewards/spend")
                .header("Authorization", "Bearer " + jwt)
                .param("amount", "0.50")
                .param("note", "ice cream"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/rewards/summary")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEarned").value(1.0))
            .andExpect(jsonPath("$.totalSpent").value(0.5))
            .andExpect(jsonPath("$.currentBalance").value(0.5));
    }
}
