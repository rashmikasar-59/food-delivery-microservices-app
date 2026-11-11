package com.fooddelivery.user.service;

import com.fooddelivery.user.dto.UpdateUserProfileRequest;
import com.fooddelivery.user.entity.UserProfile;
import com.fooddelivery.user.exception.ResourceNotFoundException;
import com.fooddelivery.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    UserProfileRepository repository;

    @InjectMocks
    UserProfileService service;

    @Test
    void getUserByIdShouldReturnProfile() {
        UserProfile profile = new UserProfile(
                "user-1", "Test User", "test@example.com",
                "CUSTOMER", LocalDateTime.now());

        when(repository.findByUserId("user-1"))
                .thenReturn(Optional.of(profile));

        UserProfile result = service.getUserById("user-1");

        assertEquals("user-1", result.getUserId());
        assertEquals("Test User", result.getName());
        verify(repository).findByUserId("user-1");
    }

    @Test
    void getUserByIdShouldThrowWhenProfileMissing() {
        when(repository.findByUserId("missing"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getUserById("missing"));
    }

    @Test
    void updateUserShouldTrimNameAndSaveProfile() {
        UserProfile profile = new UserProfile(
                "user-1", "Old Name", "test@example.com",
                "CUSTOMER", LocalDateTime.now());

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("  New Name  ");

        when(repository.findByUserId("user-1"))
                .thenReturn(Optional.of(profile));
        when(repository.save(profile)).thenReturn(profile);

        UserProfile result = service.updateUser("user-1", request);

        assertEquals("New Name", result.getName());
        verify(repository).save(profile);
    }

    @Test
    void updateUserShouldNotSaveWhenProfileMissing() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("New Name");

        when(repository.findByUserId("missing"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateUser("missing", request));

        verify(repository, never()).save(any(UserProfile.class));
    }

    @Test
    void getAllUsersShouldReturnPageWithRequestedSorting() {
        UserProfile profile = new UserProfile(
                "user-1", "Test User", "test@example.com",
                "CUSTOMER", LocalDateTime.now());

        Page<UserProfile> expected =
                new PageImpl<>(List.of(profile));

        PageRequest expectedPage = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.ASC, "name"));

        when(repository.findAll(expectedPage)).thenReturn(expected);

        Page<UserProfile> result =
                service.getAllUsers(0, 10, "name", "ASC");

        assertEquals(1, result.getTotalElements());
        assertEquals("Test User", result.getContent().get(0).getName());
        verify(repository).findAll(expectedPage);
    }

    @Test
    void getAllUsersShouldRejectNegativePage() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllUsers(-1, 10, "name", "ASC"));

        verifyNoInteractions(repository);
    }

    @Test
    void getAllUsersShouldRejectInvalidPageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllUsers(0, 51, "name", "ASC"));

        verifyNoInteractions(repository);
    }

    @Test
    void getAllUsersShouldRejectInvalidSortField() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllUsers(0, 10, "password", "ASC"));

        verifyNoInteractions(repository);
    }

    @Test
    void getAllUsersShouldRejectInvalidSortDirection() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getAllUsers(0, 10, "name", "SIDEWAYS"));

        verifyNoInteractions(repository);
    }
}
