package com.fooddelivery.auth.security;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsernameShouldReturnUserDetails() {
        User user = new User(
                "Test User", "test@example.com",
                "encoded-password", Role.CUSTOMER);
        user.setId("user-123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        CustomUserDetails result = (CustomUserDetails)
                userDetailsService.loadUserByUsername("test@example.com");

        assertEquals("test@example.com", result.getUsername());
        assertEquals("encoded-password", result.getPassword());
        assertEquals("user-123", result.getUser().getId());
        assertEquals("ROLE_CUSTOMER",
                result.getAuthorities().iterator().next().getAuthority());

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void loadUserByUsernameShouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com"));

        assertEquals(
                "User not found with email: missing@example.com",
                exception.getMessage());

        verify(userRepository).findByEmail("missing@example.com");
    }
}
