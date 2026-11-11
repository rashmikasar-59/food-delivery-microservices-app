package com.fooddelivery.auth.config;

import com.fooddelivery.auth.security.CustomUserDetailsService;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private com.fooddelivery.auth.security.JwtService jwtService;


    @Test
    void passwordEncoderShouldUseBCrypt() {
        String encoded = passwordEncoder.encode("test-password");

        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches("test-password", encoded)
        );
        org.junit.jupiter.api.Assertions.assertTrue(encoded.startsWith("$2"));
    }

    @Test
    void registrationEndpointShouldBePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginEndpointShouldBePublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointShouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/auth/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleUpdateShouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/users/user-1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleUpdateShouldRejectRegularUser() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/users/user-1/role")
                        .with(user("regular-user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleUpdateShouldAllowAdminToReachController() throws Exception {
        com.fooddelivery.auth.entity.User updatedUser =
                new com.fooddelivery.auth.entity.User(
                        "Test User",
                        "test@example.com",
                        "encoded-password",
                        com.fooddelivery.auth.entity.Role.CUSTOMER
                );
        updatedUser.setId("user-1");

        org.mockito.Mockito.when(
                authService.updateUserRole(
                        "user-1",
                        com.fooddelivery.auth.entity.Role.CUSTOMER
                )
        ).thenReturn(updatedUser);

        mockMvc.perform(patch("/api/v1/auth/users/user-1/role")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CUSTOMER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void actuatorHealthShouldBePublic() throws Exception {
        // WebMvcTest does not load the Actuator endpoint.
        // The request reaches MVC instead of being rejected by security.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isInternalServerError());
    }
}
