package com.auth.controller;

import com.auth.dto.UserResponse;
import com.auth.dto.UserDto;
import com.auth.model.UserRole;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import java.util.Locale;
import com.auth.model.DeliveryAgent;
import com.auth.model.UserCredential;
import com.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    @Autowired
    private AuthService authService;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse getUser(@PathVariable("id") String id, Authentication authentication) {
        UserResponse response = authService.getUser(id);
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!admin && response.getUser() != null && response.getUser().getEmail() != null
                && !response.getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only view your own profile");
        }
        return response;
    }

    @PutMapping()
    @ResponseStatus(HttpStatus.OK)
    public UserResponse updateUser(@RequestBody UserCredential user, Authentication authentication) {
        // Only user can update his/her details check if the User in request is same as the loggedInUser or not
        if (user == null || user.getUsername() == null || !user.getUsername().equalsIgnoreCase(authentication.getName())) {
            return UserResponse.builder()
                    .responseCode(403)
                    .msg("Access Denied")
                    .build();
        }
        return authService.updateUser(user);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public void assignRole(@PathVariable String id, @RequestParam String role) {
        // User Service is the single source of truth and publishes the Kafka sync event.
        throw new ResponseStatusException(HttpStatus.GONE,
                "Assign roles through User Service: PATCH /api/v1/user/{id}/role?role=...");
    }

    @GetMapping("/delivery")
    public List<DeliveryAgent> getDeliveryAgents() {
        return authService.getDeliveryAgents();
    }

    @GetMapping("/role")
    public String getUserRole(String username) {
        return authService.getUserRole(username);
    }
}
