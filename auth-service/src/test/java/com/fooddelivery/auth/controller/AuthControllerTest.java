package com.fooddelivery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.*;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.auth.security.JwtAuthenticationFilter;
import com.fooddelivery.auth.security.CustomUserDetailsService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.fooddelivery.auth.exception.GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void registerShouldReturnCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse(
                        "test-token", "user-123", "Test User",
                        "test@example.com", Role.CUSTOMER));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.userId").value("user-123"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void registerShouldRejectInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("");
        request.setEmail("invalid-email");
        request.setPassword("short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void loginShouldReturnOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse(
                        "login-token", "user-123", "Test User",
                        "test@example.com", Role.CUSTOMER));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-token"))
                .andExpect(jsonPath("$.userId").value("user-123"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void loginShouldRejectInvalidRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("not-an-email");
        request.setPassword("");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());

        verifyNoInteractions(authService);
    }

    @Test
    void updateUserRoleShouldReturnOk() throws Exception {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.CUSTOMER);

        User user = new User(
                "Test User", "test@example.com",
                "encoded-password", Role.CUSTOMER);
        user.setId("user-123");

        when(authService.updateUserRole("user-123", Role.CUSTOMER))
                .thenReturn(user);

        mockMvc.perform(patch("/api/v1/auth/users/user-123/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        verify(authService).updateUserRole("user-123", Role.CUSTOMER);
    }

    @Test
    void updateUserRoleShouldRejectMissingRole() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/users/user-123/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());

        verifyNoInteractions(authService);
    }
}
