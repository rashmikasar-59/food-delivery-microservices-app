package com.fooddelivery.user.controller;

import com.fooddelivery.user.dto.UpdateUserProfileRequest;
import com.fooddelivery.user.entity.UserProfile;
import com.fooddelivery.user.exception.GlobalExceptionHandler;
import com.fooddelivery.user.exception.ResourceNotFoundException;
import com.fooddelivery.user.security.JwtAuthenticationFilter;
import com.fooddelivery.user.service.UserProfileService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken authentication(
            String email, String role) {
        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private UserProfile profile() {
        return new UserProfile(
                "user-1",
                "Test User",
                "test@example.com",
                "CUSTOMER",
                LocalDateTime.now()
        );
    }

    @Test
    void getUserByIdShouldReturnProfileForOwner() throws Exception {
        when(userProfileService.getUserById("user-1"))
                .thenReturn(profile());

        mockMvc.perform(get("/api/v1/users/user-1")
                        .principal(authentication(
                                "test@example.com", "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void getUserByIdShouldRejectAnotherUser() throws Exception {
        when(userProfileService.getUserById("user-1"))
                .thenReturn(profile());

        mockMvc.perform(get("/api/v1/users/user-1")
                        .principal(authentication(
                                "other@example.com", "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdShouldAllowAdmin() throws Exception {
        when(userProfileService.getUserById("user-1"))
                .thenReturn(profile());

        mockMvc.perform(get("/api/v1/users/user-1")
                        .principal(authentication(
                                "admin@example.com", "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void updateUserShouldReturnUpdatedProfileForOwner() throws Exception {
        UserProfile existing = profile();
        UserProfile updated = profile();
        updated.setName("Updated User");

        when(userProfileService.getUserById("user-1"))
                .thenReturn(existing);
        when(userProfileService.updateUser(
                eq("user-1"), any(UpdateUserProfileRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/user-1")
                        .principal(authentication(
                                "test@example.com", "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated User\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"));

        verify(userProfileService).updateUser(
                eq("user-1"), any(UpdateUserProfileRequest.class));
    }

    @Test
    void updateUserShouldRejectAnotherUser() throws Exception {
        when(userProfileService.getUserById("user-1"))
                .thenReturn(profile());

        mockMvc.perform(put("/api/v1/users/user-1")
                        .principal(authentication(
                                "other@example.com", "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated User\"}"))
                .andExpect(status().isForbidden());

        verify(userProfileService, never()).updateUser(
                anyString(), any(UpdateUserProfileRequest.class));
    }

    @Test
    void updateUserShouldRejectInvalidName() throws Exception {
        mockMvc.perform(put("/api/v1/users/user-1")
                        .principal(authentication(
                                "test@example.com", "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(userProfileService);
    }

    @Test
    void getAllUsersShouldReturnPaginatedProfiles() throws Exception {
        when(userProfileService.getAllUsers(0, 10, "registeredAt", "DESC"))
                .thenReturn(new PageImpl<>(
                        List.of(profile()),
                        PageRequest.of(0, 10),
                        1
                ));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value("user-1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getUserByIdShouldReturnNotFoundWhenProfileIsMissing()
            throws Exception {
        when(userProfileService.getUserById("missing"))
                .thenThrow(new ResourceNotFoundException(
                        "User not found with ID: missing"));

        mockMvc.perform(get("/api/v1/users/missing")
                        .principal(authentication(
                                "test@example.com", "ROLE_CUSTOMER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("User not found with ID: missing"));
    }
}
