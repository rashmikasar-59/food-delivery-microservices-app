package com.userservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.Key;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private Key signingKey() {
        String secret = System.getenv("JWT_SECRET_BASE64");
        if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET_BASE64 is not configured");
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = Jwts.parserBuilder().setSigningKey(signingKey()).build()
                        .parseClaimsJws(header.substring(7).trim()).getBody();
                String subject = claims.getSubject();
                String role = claims.get("role", String.class);
                if (subject == null || subject.isBlank() || role == null || role.isBlank()) throw new IllegalArgumentException("JWT identity/role missing");
                var auth = new UsernamePasswordAuthenticationToken(subject, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ex) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
