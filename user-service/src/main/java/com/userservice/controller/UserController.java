package com.userservice.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.userservice.dto.UserDto;
import com.userservice.dto.UserResponse;
import com.userservice.dto.UserRegistrationResponse;
import com.userservice.model.User;
import com.userservice.model.UserRole;
import com.userservice.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/user")
@RequiredArgsConstructor
public class UserController {
	private final UserService userService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserRegistrationResponse registerUser(@RequestBody User user) {
		return userService.createUser(user);
	}

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
	public List<UserDto> getAllUsers() {
		return userService.getAllUsers();
	}

	@GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
	@ResponseStatus(HttpStatus.OK)
	public List<UserDto> getUsersByRole(@PathVariable("role") String role) {
		return userService.getUsersByRole(role);
	}

	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public UserResponse getUser(@PathVariable("id") String id, Authentication authentication) {
        UserResponse response = userService.getUser(id);
        if (response.getUser() == null) return response;
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin && !authentication.getName().equalsIgnoreCase(response.getUser().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only view your own profile");
        }
		return response;
	}

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public UserDto assignRole(@PathVariable String id, @RequestParam String role) {
        final UserRole parsedRole;
        try {
            parsedRole = UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid role. Use RESTAURANT_OWNER or DELIVERY_AGENT");
        }
        return userService.assignRole(id, parsedRole);
    }

	@PutMapping
	@ResponseStatus(HttpStatus.OK)
	public UserResponse updateUser(@RequestBody User user, Authentication authentication) {
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        UserResponse existing = userService.getUser(user.getId());
        if (existing.getUser() == null) return existing;
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin && !authentication.getName().equalsIgnoreCase(existing.getUser().getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You may only update your own profile");
        }
		return userService.updateUser(user);
	}
}
