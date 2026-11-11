package com.fooddelivery.order.client;

import com.fooddelivery.order.dto.MenuItemResponse;
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

class MenuServiceClientTest {

    private HttpServer server;
    private MenuServiceClient menuServiceClient;

    private int responseStatus;
    private String responseBody;

    @BeforeEach
    void setUp() throws Exception {
        responseStatus = 200;
        responseBody = """
                {
                  "id": "menu-1",
                  "restaurantId": "restaurant-1",
                  "name": "Pizza",
                  "price": 250.0,
                  "available": true
                }
                """;

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/api/v1/menu-items", exchange -> {
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

        doReturn(circuitBreaker).when(factory).create("orderMenuService");

        String baseUrl = "http://localhost:" + server.getAddress().getPort();

        menuServiceClient = new MenuServiceClient(
                RestClient.builder(), baseUrl, factory);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnMenuItemWhenMenuServiceRespondsSuccessfully() {
        MenuItemResponse response = menuServiceClient.getMenuItem("menu-1");

        assertNotNull(response);
        assertEquals("menu-1", response.getId());
        assertEquals("restaurant-1", response.getRestaurantId());
        assertEquals("Pizza", response.getName());
        assertEquals(250.0, response.getPrice());
        assertTrue(response.isAvailable());
    }

    @Test
    void shouldReturnNullWhenMenuItemDoesNotExist() {
        responseStatus = 404;

        assertNull(menuServiceClient.getMenuItem("missing-menu"));
    }

    @Test
    void shouldReturnServiceUnavailableWhenMenuServiceFails() {
        responseStatus = 500;

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> menuServiceClient.getMenuItem("menu-1")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
}
