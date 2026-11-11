package com.fooddelivery.restaurant.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void executeFilter(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }

        filter.doFilterInternal(
                request,
                new MockHttpServletResponse(),
                filterChain
        );
    }

    private Claims claims(String username, Object roles, Date expiration) {
        return io.jsonwebtoken.Jwts.claims()
                .subject(username)
                .add("roles", roles)
                .expiration(expiration)
                .build();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        executeFilter(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        executeFilter("Basic abc123");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldAuthenticateValidTokenWithRoles() throws Exception {
        when(jwtService.extractAllClaims("valid-token")).thenReturn(
                claims(
                        "owner@example.com",
                        List.of("ROLE_RESTAURANT_OWNER", "ROLE_ADMIN"),
                        new Date(System.currentTimeMillis() + 60000)
                )
        );

        executeFilter("Bearer valid-token");

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals("owner@example.com", authentication.getName());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_RESTAURANT_OWNER")));
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")));
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldClearContextWhenTokenIsInvalid() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("old-user", "password")
        );
        when(jwtService.extractAllClaims("invalid-token"))
                .thenThrow(new JwtException("Invalid token"));

        executeFilter("Bearer invalid-token");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldNotAuthenticateWhenUsernameIsMissing() throws Exception {
        when(jwtService.extractAllClaims("token")).thenReturn(
                claims(
                        null,
                        List.of("ROLE_RESTAURANT_OWNER"),
                        new Date(System.currentTimeMillis() + 60000)
                )
        );

        executeFilter("Bearer token");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateWhenExpirationIsMissing() throws Exception {
        when(jwtService.extractAllClaims("token")).thenReturn(
                claims("owner", List.of("ROLE_RESTAURANT_OWNER"), null)
        );

        executeFilter("Bearer token");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldNotAuthenticateWhenRolesClaimIsNotAList() throws Exception {
        when(jwtService.extractAllClaims("token")).thenReturn(
                claims(
                        "owner",
                        "ROLE_RESTAURANT_OWNER",
                        new Date(System.currentTimeMillis() + 60000)
                )
        );

        executeFilter("Bearer token");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldIgnoreNonStringRoleValues() throws Exception {
        when(jwtService.extractAllClaims("token")).thenReturn(
                claims(
                        "owner",
                        List.of("ROLE_RESTAURANT_OWNER", 123),
                        new Date(System.currentTimeMillis() + 60000)
                )
        );

        executeFilter("Bearer token");

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals(1, authentication.getAuthorities().size());
        assertEquals(
                "ROLE_RESTAURANT_OWNER",
                authentication.getAuthorities().iterator().next().getAuthority()
        );
    }
}
