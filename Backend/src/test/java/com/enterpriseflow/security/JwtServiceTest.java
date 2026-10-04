package com.enterpriseflow.security;

import com.enterpriseflow.config.JwtProperties;
import com.enterpriseflow.entity.User;
import com.enterpriseflow.entity.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "unit-test-secret-that-is-longer-than-thirty-two-characters";

    @Test
    void generatesAndValidatesTokenWithUserClaims() {
        JwtService jwtService = new JwtService(new JwtProperties(TEST_SECRET, 60_000));
        User user = User.create("Enterprise User", "user@example.com", "not-a-real-hash", UserRole.MEMBER);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).contains("user@example.com");
        assertThat(jwtService.extractRole(token)).contains("MEMBER");
    }

    @Test
    void rejectsMalformedAndExpiredTokens() {
        JwtService validService = new JwtService(new JwtProperties(TEST_SECRET, 60_000));
        JwtService expiredService = new JwtService(new JwtProperties(TEST_SECRET, -1));
        User user = User.create("Enterprise User", "user@example.com", "not-a-real-hash", UserRole.MEMBER);

        assertThat(validService.isTokenValid("not-a-jwt")).isFalse();
        assertThat(validService.isTokenValid(expiredService.generateToken(user))).isFalse();
    }
}
