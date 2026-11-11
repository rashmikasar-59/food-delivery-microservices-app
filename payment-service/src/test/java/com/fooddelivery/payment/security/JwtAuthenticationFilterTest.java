package com.fooddelivery.payment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        executeFilter(new MockHttpServletRequest());

        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic abc");

        executeFilter(request);

        verifyNoInteractions(jwtService);
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldAuthenticateUserWhenTokenIsValid() throws Exception {
        MockHttpServletRequest request = bearerRequest("valid-token");
        Claims claims = claims("customer", List.of("ROLE_USER", "ROLE_CUSTOMER"));

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractAllClaims("valid-token")).thenReturn(claims);

        executeFilter(request);

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        assertNotNull(authentication);
        assertEquals("customer", authentication.getName());
        assertEquals(2, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = bearerRequest("invalid-token");

        when(jwtService.isTokenValid("invalid-token")).thenReturn(false);

        executeFilter(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractAllClaims(anyString());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldNotAuthenticateWhenUsernameIsMissing() throws Exception {
        MockHttpServletRequest request = bearerRequest("valid-token");
        Claims claims = claims(null, List.of("ROLE_USER"));

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractAllClaims("valid-token")).thenReturn(claims);

        executeFilter(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldNotAuthenticateWhenRolesClaimIsNotAList() throws Exception {
        MockHttpServletRequest request = bearerRequest("valid-token");
        Claims claims = claims("customer", "ROLE_USER");

        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractAllClaims("valid-token")).thenReturn(claims);

        executeFilter(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldNotReplaceExistingAuthentication() throws Exception {
        var existingAuthentication =
                new UsernamePasswordAuthenticationToken("existing-user", null, List.of());

        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        MockHttpServletRequest request = bearerRequest("valid-token");
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);

        executeFilter(request);

        assertSame(existingAuthentication,
                SecurityContextHolder.getContext().getAuthentication());

        verify(jwtService, never()).extractAllClaims(anyString());
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldClearSecurityContextWhenTokenProcessingThrowsException() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing-user", null, List.of()));

        MockHttpServletRequest request = bearerRequest("broken-token");

        when(jwtService.isTokenValid("broken-token"))
                .thenThrow(new RuntimeException("Invalid token"));

        executeFilter(request);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(any(), any());
    }

    private void executeFilter(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private Claims claims(String username, Object roles) {
        return Jwts.claims().subject(username).add("roles", roles).build();
    }
}
