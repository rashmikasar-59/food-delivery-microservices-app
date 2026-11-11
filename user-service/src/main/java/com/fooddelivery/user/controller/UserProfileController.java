
package com.fooddelivery.user.controller;

import com.fooddelivery.user.dto.UpdateUserProfileRequest;
import com.fooddelivery.user.entity.UserProfile;
import com.fooddelivery.user.service.UserProfileService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{userId}")
    public UserProfile getUserById(
            @PathVariable String userId,
            Authentication authentication) {

        UserProfile profile = userProfileService.getUserById(userId);

        checkOwnershipOrAdmin(profile, authentication);

        return profile;
    }

    @PutMapping("/{userId}")
    public UserProfile updateUser(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserProfileRequest request,
            Authentication authentication) {

        UserProfile profile = userProfileService.getUserById(userId);

        checkOwnershipOrAdmin(profile, authentication);

        return userProfileService.updateUser(userId, request);
    }

    @GetMapping
    public Page<UserProfile> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "registeredAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        return userProfileService.getAllUsers(
                page, size, sortBy, direction);
    }

    private void checkOwnershipOrAdmin(
            UserProfile profile,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = profile.getEmail()
                .equalsIgnoreCase(authentication.getName());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another user's profile"
            );
        }
    }
}
