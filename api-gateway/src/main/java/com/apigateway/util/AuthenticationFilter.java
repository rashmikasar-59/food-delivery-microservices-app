
package com.apigateway.util;

import java.util.List;

import com.apigateway.config.RouteValidator;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFilter
        extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator routeValidator;
    private final JwtUtil jwtUtil;

    public AuthenticationFilter(
            RouteValidator routeValidator,
            JwtUtil jwtUtil) {
        super(Config.class);
        this.routeValidator = routeValidator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            // Remove client-supplied identity headers even on public routes.
            var sanitizedRequest = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("loggedInUser");
                        headers.remove("loggedInRole");
                    })
                    .build();

            var sanitizedExchange = exchange.mutate()
                    .request(sanitizedRequest)
                    .build();

            if (!routeValidator.isSecured(sanitizedExchange.getRequest())) {
                return chain.filter(sanitizedExchange);
            }

            List<String> authHeaders = sanitizedExchange.getRequest()
                    .getHeaders()
                    .getOrEmpty(HttpHeaders.AUTHORIZATION);

            if (authHeaders.isEmpty()
                    || authHeaders.get(0) == null
                    || !authHeaders.get(0).startsWith("Bearer ")) {
                sanitizedExchange.getResponse()
                        .setStatusCode(HttpStatus.UNAUTHORIZED);
                return sanitizedExchange.getResponse().setComplete();
            }

            String token = authHeaders.get(0).substring(7).trim();

            if (token.isBlank() || !jwtUtil.isTokenValid(token)) {
                sanitizedExchange.getResponse()
                        .setStatusCode(HttpStatus.UNAUTHORIZED);
                return sanitizedExchange.getResponse().setComplete();
            }

            try {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                if (username == null || username.isBlank()
                        || role == null || role.isBlank()) {
                    sanitizedExchange.getResponse()
                            .setStatusCode(HttpStatus.UNAUTHORIZED);
                    return sanitizedExchange.getResponse().setComplete();
                }

                var authenticatedRequest = sanitizedExchange.getRequest()
                        .mutate()
                        .headers(headers -> {
                            headers.set("loggedInUser", username);
                            headers.set("loggedInRole", role);
                        })
                        .build();

                return chain.filter(
                        sanitizedExchange.mutate()
                                .request(authenticatedRequest)
                                .build());

            } catch (io.jsonwebtoken.JwtException
                    | IllegalArgumentException exception) {
                sanitizedExchange.getResponse()
                        .setStatusCode(HttpStatus.UNAUTHORIZED);
                return sanitizedExchange.getResponse().setComplete();
            }
        };
    }

    public static class Config {
        // No additional configuration required.
    }
}
