package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.*;
import com.fooddelivery.auth.entity.*;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock AuthEventProducer authEventProducer;

    @InjectMocks AuthService authService;

    @Test
    void registerShouldCreateCustomerAndPublishEvent() {
        RegisterRequest request = new RegisterRequest();
        request.setName("  Test User  ");
        request.setEmail("  TEST@EXAMPLE.COM  ");
        request.setPassword("password123");

        User savedUser = new User(
                "Test User", "test@example.com",
                "encoded-password", Role.CUSTOMER);
        savedUser.setId("user-123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("test-jwt");

        AuthResponse response = authService.register(request);

        assertEquals("user-123", response.getUserId());
        assertEquals("Test User", response.getName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertEquals("test-jwt", response.getToken());

        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(user ->
                user.getName().equals("Test User")
                && user.getEmail().equals("test@example.com")
                && user.getPassword().equals("encoded-password")
                && user.getRole() == Role.CUSTOMER));
        verify(authEventProducer).publishUserRegisteredEvent(any());
        verify(jwtService).generateToken(any(CustomUserDetails.class));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("TEST@EXAMPLE.COM");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Email is already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, authEventProducer, jwtService);
    }

    @Test
    void loginShouldReturnTokenForAuthenticatedUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("  TEST@EXAMPLE.COM ");
        request.setPassword("password123");

        User user = new User(
                "Test User", "test@example.com",
                "encoded-password", Role.CUSTOMER);
        user.setId("user-123");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new CustomUserDetails(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn("login-jwt");

        AuthResponse response = authService.login(request);

        assertEquals("login-jwt", response.getToken());
        assertEquals("user-123", response.getUserId());
        assertEquals("test@example.com", response.getEmail());

        verify(authenticationManager).authenticate(argThat(token ->
                token.getPrincipal().equals("test@example.com")
                && token.getCredentials().equals("password123")));
        verify(jwtService).generateToken(any(CustomUserDetails.class));
    }

    @Test
    void loginShouldPropagateInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(request));

        verifyNoInteractions(jwtService);
    }

    @Test
    void updateUserRoleShouldSaveNewRole() {
        User user = new User(
                "Test User", "test@example.com",
                "encoded-password", Role.CUSTOMER);
        user.setId("user-123");

        when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updatedUser = authService.updateUserRole("user-123", Role.CUSTOMER);

        assertEquals(Role.CUSTOMER, updatedUser.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRoleShouldRejectAdminRole() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.updateUserRole("user-123", Role.ADMIN));

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateUserRoleShouldRejectNullRole() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.updateUserRole("user-123", null));

        verifyNoInteractions(userRepository);
    }

    @Test
    void updateUserRoleShouldFailWhenUserDoesNotExist() {
        when(userRepository.findById("missing-user")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.updateUserRole("missing-user", Role.CUSTOMER));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
