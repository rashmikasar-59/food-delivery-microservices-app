package com.restaurantservice.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Correlation-ID";
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || id.isBlank() || id.length() > 100) id = UUID.randomUUID().toString();
        response.setHeader(HEADER, id);
        try { org.slf4j.MDC.put("correlationId", id); chain.doFilter(request, response); }
        finally { org.slf4j.MDC.remove("correlationId"); }
    }
}
