package com.fooddelivery.auth.config;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrapConfig config;

    private CommandLineRunner runner;

    @BeforeEach
    void setUp() {
        runner = config.createInitialAdmin(
                userRepository,
                passwordEncoder,
                " ADMIN@EXAMPLE.COM ",
                "secure-password"
        );
    }

    @Test
    void shouldSkipWhenCredentialsAreMissing() {
        CommandLineRunner emptyEmailRunner = config.createInitialAdmin(
                userRepository, passwordEncoder, "", "password"
        );

        assertDoesNotThrow(() -> emptyEmailRunner.run());

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void shouldSkipWhenAdminAlreadyExists() throws Exception {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        runner.run();

        verify(userRepository).existsByRole(Role.ADMIN);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectEmailBelongingToExistingUser() {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> runner.run());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldCreateInitialAdminWithEncodedPassword() throws Exception {
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secure-password")).thenReturn("encoded-password");

        runner.run();

        verify(passwordEncoder).encode("secure-password");
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("admin@example.com")
                        && user.getRole() == Role.ADMIN
                        && user.getPassword().equals("encoded-password")
        ));
    }
}
