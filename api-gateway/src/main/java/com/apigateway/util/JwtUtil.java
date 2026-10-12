
package com.apigateway.util;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil() {
        String secret = System.getenv("JWT_SECRET_BASE64");

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET_BASE64 environment variable is required"
            );
        }

        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret.trim());
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "JWT_SECRET_BASE64 must contain a valid Base64-encoded key of sufficient length",
                exception
            );
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            Claims claims = extractAllClaims(token);

            return claims.getSubject() != null
                    && !claims.getSubject().isBlank()
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date())
                    && claims.get("role", String.class) != null;
        } catch (Exception exception) {
            return false;
        }
    }
}
