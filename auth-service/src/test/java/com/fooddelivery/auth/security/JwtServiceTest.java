package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef"
                    .getBytes());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);

        User user = new User(
                "Test User",
                "test@example.com",
                "encoded-password",
                Role.CUSTOMER);

        user.setId("user-123");
        userDetails = new CustomUserDetails(user);
    }

    @Test
    void generateTokenShouldCreateValidToken() {
        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void extractUsernameShouldReturnCorrectEmail() {
        String token = jwtService.generateToken(userDetails);

        assertEquals(
                "test@example.com",
                jwtService.extractUsername(token));
    }

    @Test
    void extractExpirationShouldReturnFutureDate() {
        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.extractExpiration(token).getTime()
                > System.currentTimeMillis());
    }

    @Test
    void isTokenValidShouldRejectDifferentUsername() {
        String token = jwtService.generateToken(userDetails);

        User anotherUser = new User(
                "Another User",
                "another@example.com",
                "encoded-password",
                Role.CUSTOMER);

        CustomUserDetails anotherDetails =
                new CustomUserDetails(anotherUser);

        assertFalse(jwtService.isTokenValid(token, anotherDetails));
    }

    @Test
    void isTokenValidShouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);

        String expiredToken = jwtService.generateToken(userDetails);

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    void extractUsernameShouldRejectTamperedToken() {
        String token = jwtService.generateToken(userDetails);
        String tamperedToken = token + "invalid";

        assertThrows(
                JwtException.class,
                () -> jwtService.extractUsername(tamperedToken));
    }
}
