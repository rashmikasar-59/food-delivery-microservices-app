
package com.userservice.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.userservice.dto.UserDto;
import com.userservice.dto.UserResponse;
import com.userservice.dto.UserRegistrationResponse;
import com.userservice.model.User;
import com.userservice.model.UserRole;
import com.userservice.repository.UserRepository;
import com.userservice.event.UserRegisteredEvent;
import com.userservice.event.UserRegisteredPublisher;
import com.userservice.event.UserRoleUpdatedPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRegisteredPublisher userRegisteredPublisher;
    private final UserRoleUpdatedPublisher userRoleUpdatedPublisher;

    // Register user
    public UserRegistrationResponse createUser(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        user.setEmail(user.getEmail().trim().toLowerCase(Locale.ROOT));
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }
        // Public registration never assigns privileged roles.
        user.setUserRole(UserRole.CUSTOMER);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User saved = userRepository.save(user);
        // Publish the same encoded password so auth-service can authenticate without re-hashing.
        userRegisteredPublisher.publish(new UserRegisteredEvent(saved.getId(), saved.getFullName(),
                saved.getEmail(), saved.getPassword(), saved.getPhoneNumber(), saved.getUserRole().name()));
        return UserRegistrationResponse.builder()
                .id(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .userRole(saved.getUserRole())
                .message("Registration successful. Authentication record is synchronizing through Kafka.")
                .build();
    }

    public UserDto assignRole(String userId, UserRole role) {
        if (role != UserRole.RESTAURANT_OWNER && role != UserRole.DELIVERY_AGENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only RESTAURANT_OWNER or DELIVERY_AGENT can be assigned");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setUserRole(role);
        User saved = userRepository.save(user);
        // Synchronize Auth Service so the next login receives the newly assigned role in its JWT.
        userRoleUpdatedPublisher.publish(new UserRegisteredEvent(
                saved.getId(), saved.getFullName(), saved.getEmail(), saved.getPassword(),
                saved.getPhoneNumber(), saved.getUserRole().name()));
        return mapUserToDto(saved);
    }

    // Get user by ID
    public UserResponse getUser(String id) {
        Optional<User> user = userRepository.findById(id);

        if (user.isPresent()) {
            return mapUserToUserResponse(user.get());
        }

        return UserResponse.builder()
                .responseCode(404)
                .msg("User with given id is not present")
                .build();
    }

    // Get all users
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapUserToDto)
                .toList();
    }

    // Get users by role: CUSTOMER, RESTAURANT_MANAGER, etc.

    public List<UserDto> getUsersByRole(String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase(Locale.ROOT));

            return userRepository.findByUserRole(userRole)
                    .stream()
                    .map(this::mapUserToDto)
                    .toList();

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role: " + role);
        }
    }
    // Update user
    public UserResponse updateUser(User user) {

        if (user.getId() == null) {
            return UserResponse.builder()
                    .responseCode(400)
                    .msg("Please provide user id")
                    .build();
        }

        Optional<User> userOptional =
                userRepository.findById(user.getId());

        if (userOptional.isPresent()) {
            return updateUser(userOptional.get(), user);
        }

        return UserResponse.builder()
                .responseCode(404)
                .msg("User with given id is not present")
                .build();
    }

    private UserResponse updateUser(User existingUser, User updatedUser) {

        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
        existingUser.setAddress(updatedUser.getAddress());

        userRepository.save(existingUser);

        return mapUserToUserResponse(existingUser);
    }

    // Map User to UserResponse
    private UserResponse mapUserToUserResponse(User user) {
        return UserResponse.builder()
                .user(mapUserToDto(user))
                .responseCode(200)
                .msg("Success")
                .build();
    }

    // Map User to UserDto
    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .userRole(user.getUserRole())
                .build();
    }
}