package com.auth.service;

import com.auth.dto.UserDto;
import com.auth.dto.UserResponse;
import com.auth.model.DeliveryAgent;
import com.auth.model.UserCredential;
import com.auth.model.UserRole;
import com.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder  passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public String saveUser(UserCredential credential) {
        if (credential == null || credential.getUsername() == null || credential.getUsername().isBlank()
                || credential.getPassword() == null || credential.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
        String normalizedUsername = credential.getUsername().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
        // Never accept a privileged role from public registration input.
        credential.setUsername(normalizedUsername);
        credential.setUserRole(UserRole.CUSTOMER);
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        userRepository.save(credential);
        return "User added to the system as CUSTOMER";
    }

    public UserDto assignRole(String userId, UserRole role) {
        if (role != UserRole.RESTAURANT_OWNER && role != UserRole.DELIVERY_AGENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only RESTAURANT_OWNER or DELIVERY_AGENT can be assigned through this endpoint");
        }
        UserCredential user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setUserRole(role);
        return mapUserToDto(userRepository.save(user));
    }

    private UserDto mapUserToDto(UserCredential user) {
        return UserDto.builder().id(user.getId()).fullName(user.getFullName())
                .email(user.getUsername()).phoneNumber(user.getPhoneNumber())
                .address(user.getAddress()).userRole(user.getUserRole()).build();
    }

    public String generateToken(String username) {
        UserCredential credential = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return jwtService.generateToken(username, credential.getUserRole() == null ? "CUSTOMER" : credential.getUserRole().name());
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }

    public UserResponse getUser(String id) {
        Optional<UserCredential> user = userRepository.findById(id);
        if(user.isPresent()) {
            UserCredential user1 = user.get();
            return mapUserToUserResponse(user1);
        }
        return UserResponse.builder()
                .responseCode(404)
                .msg("User with given id is not present")
                .build();
    }

    public UserResponse updateUser(UserCredential user) {
        if(user.getId() == null) {
            return UserResponse.builder()
                    .responseCode(400)
                    .msg("Please provide user id")
                    .build();
        }
        Optional<UserCredential> userOptional = userRepository.findById(user.getId());
        if(userOptional.isPresent()) {
            return updateUser(userOptional.get(), user);
        }
        return UserResponse.builder()
                .responseCode(404)
                .msg("User with given id is not present")
                .build();
    }

    private UserResponse updateUser(UserCredential user, UserCredential userNew) {
        user.setFullName(userNew.getFullName());
        user.setUsername(userNew.getUsername());
        user.setPhoneNumber(userNew.getPhoneNumber());
        user.setAddress(userNew.getAddress());
        userRepository.save(user);
        return mapUserToUserResponse(user);
    }

    private UserResponse mapUserToUserResponse(UserCredential user1) {
        return UserResponse.builder()
                .user(UserDto.builder()
                        .id(user1.getId())
                        .fullName(user1.getFullName())
                        .email(user1.getUsername())
                        .phoneNumber(user1.getPhoneNumber())
                        .address(user1.getAddress())
                        .userRole(user1.getUserRole())
                        .build())
                .responseCode(200)
                .msg("Success")
                .build();
    }

    public List<DeliveryAgent> getDeliveryAgents() {
        List<UserCredential> allUsers = userRepository.findAll();
        return allUsers.stream()
                .filter(user -> user.getUserRole() == UserRole.DELIVERY_AGENT)
                .map(user -> DeliveryAgent.builder()
                .name(user.getFullName())
                .address(user.getAddress())
                .phoneNumber(user.getPhoneNumber())
                .build()).toList();
    }

    public String getUserRole(String username) {
        Optional<UserCredential> userCredential = userRepository.findByUsername(username);
        return userCredential.map(credential -> credential.getUserRole() == null ? "CUSTOMER" : credential.getUserRole().name()).orElse(null);
    }
}
