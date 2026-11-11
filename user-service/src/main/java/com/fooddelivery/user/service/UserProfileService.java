package com.fooddelivery.user.service;

import com.fooddelivery.user.dto.UpdateUserProfileRequest;
import com.fooddelivery.user.entity.UserProfile;
import com.fooddelivery.user.exception.ResourceNotFoundException;
import com.fooddelivery.user.repository.UserProfileRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("name", "email", "role", "registeredAt");

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfile getUserById(String userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with ID: " + userId));
    }

    public UserProfile updateUser(
            String userId,
            UpdateUserProfileRequest request) {

        UserProfile user = getUserById(userId);

        user.setName(request.getName().trim());

        return repository.save(user);
    }

    public Page<UserProfile> getAllUsers(
            int page,
            int size,
            String sortBy,
            String direction) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (size < 1 || size > 50) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 50");
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy);
        }

        Sort.Direction sortDirection =
                Sort.Direction.fromString(direction);

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        return repository.findAll(pageable);
    }
}
