package com.example.readingrewards.auth;

import com.example.readingrewards.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilUnitTests {

    @Test
    void generatesAndValidatesTokenWithBase64Secret() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("JWT_SECRET", "cmVhZGluZy1yZXdhcmRzLXNwZWMtdGVzdC1zZWNyZXQta2V5LTIwMjY=");

        JwtUtil jwtUtil = new JwtUtil(env);
        String token = jwtUtil.generateToken("kid-user");

        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("kid-user", jwtUtil.extractUsername(token));
    }

    @Test
    void fallsBackToJwtSecretPropertyWhenJwtSecretEnvMissing() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("jwt.secret", "plain-text-jwt-secret-that-is-long-enough-for-hs256-testing");

        JwtUtil jwtUtil = new JwtUtil(env);
        String token = jwtUtil.generateToken("parent@example.com");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("parent@example.com", jwtUtil.extractUsername(token));
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("JWT_SECRET", "cmVhZGluZy1yZXdhcmRzLXNwZWMtdGVzdC1zZWNyZXQta2V5LTIwMjY=");

        JwtUtil jwtUtil = new JwtUtil(env);

        assertFalse(jwtUtil.validateToken("not-a-jwt-token"));
    }

    @Test
    void throwsWhenNoJwtSecretConfigured() {
        MockEnvironment env = new MockEnvironment();

        assertThrows(IllegalStateException.class, () -> new JwtUtil(env));
    }
}
