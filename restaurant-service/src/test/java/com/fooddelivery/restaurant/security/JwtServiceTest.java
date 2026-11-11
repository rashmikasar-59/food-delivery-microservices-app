package com.fooddelivery.restaurant.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String TEST_SECRET =
            Base64.getEncoder().encodeToString(
                    "01234567890123456789012345678901".getBytes()
            );

    private SecretKey signingKey;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        signingKey = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(TEST_SECRET)
        );
    }

    private String createToken(Date expiration) {
        return Jwts.builder()
                .subject("restaurant-owner")
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    @Test
    void shouldExtractAllClaims() {
        String token = createToken(
                new Date(System.currentTimeMillis() + 60000)
        );

        Claims claims = jwtService.extractAllClaims(token);

        assertEquals("restaurant-owner", claims.getSubject());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void shouldExtractUsername() {
        String token = createToken(
                new Date(System.currentTimeMillis() + 60000)
        );

        assertEquals("restaurant-owner", jwtService.extractUsername(token));
    }

    @Test
    void shouldReturnTrueForValidToken() {
        String token = createToken(
                new Date(System.currentTimeMillis() + 60000)
        );

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void shouldRejectExpiredToken() {
        String token = createToken(
                new Date(System.currentTimeMillis() - 60000)
        );

        assertThrows(
                io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token)
        );
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        SecretKey anotherKey = Keys.hmacShaKeyFor(
                "abcdefghijklmnopqrstuvwxyz123456".getBytes()
        );

        String token = Jwts.builder()
                .subject("restaurant-owner")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(anotherKey)
                .compact();

        assertThrows(
                io.jsonwebtoken.JwtException.class,
                () -> jwtService.extractAllClaims(token)
        );
    }

    @Test
    void shouldReturnFalseWhenTokenHasNoExpiration() {
        String token = Jwts.builder()
                .subject("restaurant-owner")
                .signWith(signingKey)
                .compact();

        assertFalse(jwtService.isTokenValid(token));
    }
}
