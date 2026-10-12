package com.paymentservice.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String secret = System.getenv("JWT_SECRET_BASE64");
            if (secret == null || secret.isBlank()) throw new IllegalStateException("JWT_SECRET_BASE64 is not configured");
            Claims claims = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                    .build().parseClaimsJws(header.substring(7).trim()).getBody();
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            if (username == null || username.isBlank() || role == null || role.isBlank())
                throw new IllegalArgumentException("JWT subject or role is missing");
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                @Override public String getHeader(String name) {
                    if ("loggedInUser".equalsIgnoreCase(name)) return username;
                    if ("loggedInRole".equalsIgnoreCase(name)) return role;
                    return super.getHeader(name);
                }
                @Override public Enumeration<String> getHeaders(String name) {
                    if ("loggedInUser".equalsIgnoreCase(name)) return Collections.enumeration(List.of(username));
                    if ("loggedInRole".equalsIgnoreCase(name)) return Collections.enumeration(List.of(role));
                    return super.getHeaders(name);
                }
            };
            chain.doFilter(wrapped, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
        }
    }
}
