package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private CustomUserDetailsService userDetailsService;
    private JwtAuthenticationFilter filter;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(CustomUserDetailsService.class);

        filter = new JwtAuthenticationFilter(
                jwtService, userDetailsService);

        User user = new User(
                "Test User",
                "test@example.com",
                "encoded-password",
                Role.CUSTOMER);

        user.setId("user-123");
        userDetails = new CustomUserDetails(user);

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void filterShouldContinueWhenAuthorizationHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());

        verifyNoInteractions(jwtService, userDetailsService);
    }

    @Test
    void filterShouldAuthenticateValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername("valid-token"))
                .thenReturn("test@example.com");

        when(userDetailsService.loadUserByUsername("test@example.com"))
                .thenReturn(userDetails);

        when(jwtService.isTokenValid("valid-token", userDetails))
                .thenReturn(true);

        filter.doFilter(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                "test@example.com",
                SecurityContextHolder.getContext()
                        .getAuthentication().getName());

        assertNotNull(chain.getRequest());
        verify(userDetailsService).loadUserByUsername("test@example.com");
    }

    @Test
    void filterShouldRejectInvalidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtService.extractUsername("invalid-token"))
                .thenThrow(new MalformedJwtException("Invalid token"));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString()
                .contains("Invalid or expired JWT token"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(chain.getRequest());
    }

    @Test
    void filterShouldContinueWhenBearerPrefixIsMissing()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic some-value");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        verifyNoInteractions(jwtService, userDetailsService);
    }
}
