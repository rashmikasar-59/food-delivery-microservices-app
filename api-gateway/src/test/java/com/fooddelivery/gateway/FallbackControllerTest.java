package com.fooddelivery.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackControllerTest {

    private final FallbackController fallbackController =
            new FallbackController();

    @Test
    void shouldReturnServiceUnavailableStatus() {
        ResponseEntity<Map<String, String>> response =
                fallbackController.serviceUnavailable();

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                response.getStatusCode()
        );
    }

    @Test
    void shouldReturnServiceUnavailableStatusMessage() {
        ResponseEntity<Map<String, String>> response =
                fallbackController.serviceUnavailable();

        assertNotNull(response.getBody());
        assertEquals(
                "SERVICE_UNAVAILABLE",
                response.getBody().get("status")
        );
    }

    @Test
    void shouldReturnHelpfulFallbackMessage() {
        ResponseEntity<Map<String, String>> response =
                fallbackController.serviceUnavailable();

        assertNotNull(response.getBody());
        assertEquals(
                "The requested service is temporarily unavailable. Please try again later.",
                response.getBody().get("message")
        );
    }
}
