package com.fooddelivery.order.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RestaurantServiceClientTest {

    private HttpServer server;
    private RestaurantServiceClient restaurantServiceClient;

    private int responseStatus;
    private String responseBody;

    @BeforeEach
    void setUp() throws Exception {
        responseStatus = 200;
        responseBody = "{\"id\":\"restaurant-1\",\"ownerId\":\"owner-1\"}";

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/api/v1/restaurants", exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        server.start();

        CircuitBreaker circuitBreaker = new CircuitBreaker() {
            @Override
            public <T> T run(
                    Supplier<T> supplier,
                    Function<Throwable, T> fallback) {
                try {
                    return supplier.get();
                } catch (Throwable ex) {
                    return fallback.apply(ex);
                }
            }
        };

        @SuppressWarnings("unchecked")
        CircuitBreakerFactory<?, ?> factory =
                mock(CircuitBreakerFactory.class);

        doReturn(circuitBreaker)
                .when(factory).create("orderRestaurantService");

        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        restaurantServiceClient = new RestaurantServiceClient(
                RestClient.builder(), baseUrl, factory);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnTrueWhenUserIsRestaurantOwner() {
        assertTrue(restaurantServiceClient.isRestaurantOwner(
                "restaurant-1", "owner-1"));
    }

    @Test
    void shouldReturnFalseWhenUserIsNotRestaurantOwner() {
        assertFalse(restaurantServiceClient.isRestaurantOwner(
                "restaurant-1", "owner-2"));
    }

    @Test
    void shouldReturnFalseWhenRestaurantDoesNotExist() {
        responseStatus = 404;

        assertFalse(restaurantServiceClient.isRestaurantOwner(
                "missing-restaurant", "owner-1"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenRestaurantServiceFails() {
        responseStatus = 500;

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> restaurantServiceClient.isRestaurantOwner(
                        "restaurant-1", "owner-1")
        );

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getStatusCode()
        );
    }
}
