package com.fooddelivery.restaurant.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleNotFoundReturns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(
                        new ResourceNotFoundException("Restaurant not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Restaurant not found", response.getBody().get("message"));
    }

    @Test
    void handleDuplicateReturns409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDuplicate(
                        new DuplicateResourceException("Restaurant already exists"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Restaurant already exists", response.getBody().get("message"));
    }


    @Test
    void handleDuplicateKeyReturns409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleDuplicateKey(
                        new org.springframework.dao.DuplicateKeyException("Duplicate email"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Restaurant email already exists",
                response.getBody().get("message"));
    }

    @Test
    void handleBadRequestReturns400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleBadRequest(new IllegalArgumentException("Invalid input"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input", response.getBody().get("message"));
    }

    @Test
    void handleAccessDeniedReturns403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDenied(new AccessDeniedException("Denied"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(
                "You are not authorized to perform this action",
                response.getBody().get("message"));
    }

    @Test
    void handleOtherReturns500() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleOther(new RuntimeException("Unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                "An unexpected error occurred",
                response.getBody().get("message"));
    }

    @Test
    void responsesContainTimestampAndError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(
                        new ResourceNotFoundException("Not found"));

        assertNotNull(response.getBody().get("timestamp"));
        assertEquals("Not Found", response.getBody().get("error"));
    }

    @Test
    void handleValidationReturns400WithFieldDetails() {
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(
                        new com.fooddelivery.restaurant.dto.CreateRestaurantRequest(),
                        "createRestaurantRequest");

        bindingResult.addError(new org.springframework.validation.FieldError(
                "createRestaurantRequest",
                "name",
                "",
                false,
                null,
                null,
                "Restaurant name is required"));

        org.springframework.web.bind.MethodArgumentNotValidException exception =
                new org.springframework.web.bind.MethodArgumentNotValidException(
                        null, bindingResult);

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("message"));

        @SuppressWarnings("unchecked")
        Map<String, String> details =
                (Map<String, String>) response.getBody().get("details");

        assertEquals("Restaurant name is required", details.get("name"));
    }
}
