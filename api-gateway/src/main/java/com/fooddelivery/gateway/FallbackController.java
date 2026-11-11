package com.fooddelivery.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/service-unavailable")
    public ResponseEntity<Map<String, String>> serviceUnavailable() {

        Map<String, String> response = Map.of(
                "status", "SERVICE_UNAVAILABLE",
                "message", "The requested service is temporarily unavailable. Please try again later."
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
