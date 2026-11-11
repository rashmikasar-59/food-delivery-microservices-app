
package com.fooddelivery.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/protected")
    public Map<String, Object> protectedEndpoint(
            Authentication authentication) {

        return Map.of(
                "message", "JWT authentication successful!",
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities()
        );
    }
}
