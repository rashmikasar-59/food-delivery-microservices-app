package com.fooddelivery.restaurant.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class RestAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestAccessDeniedHandler handler =
            new RestAccessDeniedHandler(objectMapper);

    @Test
    void shouldReturnForbiddenStatusAndJsonResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/restaurants/123");

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                request,
                response,
                new AccessDeniedException("Access denied")
        );

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertEquals(403, body.get("status").asInt());
        assertEquals("Forbidden", body.get("error").asText());
        assertEquals(
                "You are not authorized to perform this action",
                body.get("message").asText()
        );
        assertEquals(
                "/api/v1/restaurants/123",
                body.get("path").asText()
        );
        assertTrue(body.hasNonNull("timestamp"));
    }
}
