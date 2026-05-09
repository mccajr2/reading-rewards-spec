package com.example.readingrewards.auth;

import com.example.readingrewards.auth.service.VerificationEmailService;
import com.example.readingrewards.auth.model.User;
import com.example.readingrewards.auth.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

  @MockBean
  private VerificationEmailService verificationEmailService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
      when(verificationEmailService.sendEmail(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void parentNeedsVerificationBeforeLogin() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "parent@example.com",
                      "password": "password123",
                      "firstName": "Pat",
                      "lastName": "Parent"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "parent@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(content().string("Parent account not verified. Please check your email."));
    }

    @Test
    void verifiedParentCanLoginAndAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "verified@example.com",
                      "password": "password123",
                      "firstName": "Vera",
                      "lastName": "Verified"
                    }
                    """))
            .andExpect(status().isOk());

        User created = userRepository.findByEmail("verified@example.com").orElseThrow();

        mockMvc.perform(get("/api/auth/verify-email")
                .param("token", created.getVerificationToken()))
            .andExpect(status().isOk());

        String token = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "verified@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.role").value("PARENT"))
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/protected-ping")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(content().string("pong"));
    }

    @Test
    void childCanLoginWithUsername() throws Exception {
        User child = new User();
        child.setRole(User.UserRole.CHILD);
        child.setStatus("VERIFIED");
        child.setUsername("kid-user");
        child.setFirstName("Kid");
        child.setPassword(passwordEncoder.encode("child-pass"));
        userRepository.save(child);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "kid-user",
                      "password": "child-pass"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("kid-user"))
            .andExpect(jsonPath("$.user.role").value("CHILD"));
    }

    @Test
    void verifiedParentCanManageKidAccounts() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "manager@example.com",
                      "password": "password123",
                      "firstName": "Mina",
                      "lastName": "Manager"
                    }
                    """))
            .andExpect(status().isOk());

        User created = userRepository.findByEmail("manager@example.com").orElseThrow();

        mockMvc.perform(get("/api/auth/verify-email")
                .param("token", created.getVerificationToken()))
            .andExpect(status().isOk());

        String parentToken = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "manager@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");

        mockMvc.perform(post("/api/parent/kids")
                .header("Authorization", "Bearer " + parentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "kid-reset",
                      "firstName": "Casey",
                      "password": "old-pass"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/parent/kids")
                .header("Authorization", "Bearer " + parentToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].username").value("kid-reset"));

        mockMvc.perform(post("/api/parent/reset-child-password")
                .header("Authorization", "Bearer " + parentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "childUsername": "kid-reset",
                      "newPassword": "new-pass"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "kid-reset",
                      "password": "new-pass"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("CHILD"));
    }

        @Test
        void logoutEndpointReturnsOk() throws Exception {
          mockMvc.perform(post("/api/auth/signup")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                {
                  "email": "logout@example.com",
                  "password": "password123",
                  "firstName": "Log",
                  "lastName": "Out"
                }
                """))
            .andExpect(status().isOk());

          User created = userRepository.findByEmail("logout@example.com").orElseThrow();

          mockMvc.perform(get("/api/auth/verify-email")
              .param("token", created.getVerificationToken()))
            .andExpect(status().isOk());

          String parentToken = mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                {
                  "username": "logout@example.com",
                  "password": "password123"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString()
            .replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");

          mockMvc.perform(post("/api/auth/logout")
              .header("Authorization", "Bearer " + parentToken))
            .andExpect(status().isOk());
        }
}