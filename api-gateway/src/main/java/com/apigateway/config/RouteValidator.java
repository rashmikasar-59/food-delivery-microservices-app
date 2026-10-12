
package com.apigateway.config;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouteValidator {

    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/token",
            "/api/v1/auth/validate"
    );

    public boolean isSecured(ServerHttpRequest request) {

        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // Public authentication endpoints
        if (OPEN_API_ENDPOINTS.contains(path)) {
            return false;
        }

        // Public customer registration
        if (HttpMethod.POST.equals(method)
                && "/api/v1/user".equals(path)) {
            return false;
        }

        // Public restaurant browsing (GET only)
        if (HttpMethod.GET.equals(method)
                && (path.equals("/api/v1/restaurant")
                || path.startsWith("/api/v1/restaurant/")
                || path.equals("/api/v1/fooditem")
                || path.startsWith("/api/v1/fooditem/"))) {
            return false;
        }

        // All other endpoints require authentication
        return true;
    }
}
