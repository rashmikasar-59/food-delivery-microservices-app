package com.auth.controller;

import com.auth.dto.AuthRequest;
import com.auth.dto.UserResponse;
import com.auth.model.UserCredential;
import com.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String createUser(@RequestBody UserCredential user) {
        throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.GONE,
                "Use POST /api/v1/user for registration. User Service publishes UserRegisteredEvent to synchronize Auth Service.");
    }

    
    @PostMapping("/token")
    public String getToken(@RequestBody AuthRequest authRequest) {
        if (authRequest == null || authRequest.getUsername() == null || authRequest.getUsername().isBlank()
                || authRequest.getPassword() == null || authRequest.getPassword().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Username/email and password are required");
        }
        try {
            Authentication authenticate =
                    authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername() == null ? null : authRequest.getUsername().trim().toLowerCase(java.util.Locale.ROOT),
                            authRequest.getPassword()
                        )
                    );

            String token =
                    authService.generateToken(authRequest.getUsername().trim().toLowerCase(java.util.Locale.ROOT));

            return token;
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email/username or password");
        }
    }
    


    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        authService.validateToken(token);
        return "Token is valid";
    }

}
