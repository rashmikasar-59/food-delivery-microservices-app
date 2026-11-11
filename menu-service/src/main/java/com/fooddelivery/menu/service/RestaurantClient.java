package com.fooddelivery.menu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@Component
public class RestaurantClient {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public RestaurantClient(
            @Value("${restaurant-service.base-url}") String baseUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        this.circuitBreaker = circuitBreakerFactory.create("restaurantService");
    }

    public boolean isRestaurantOwner(String restaurantId, String username) {

        return circuitBreaker.run(
                () -> {
                    try {
                        Map<?, ?> restaurant = restClient.get()
                                .uri("/api/v1/restaurants/{id}", restaurantId)
                                .retrieve()
                                .body(Map.class);

                        if (restaurant == null) {
                            return false;
                        }

                        Object ownerId = restaurant.get("ownerId");

                        return ownerId != null
                                && ownerId.toString().equals(username);

                    } catch (HttpClientErrorException.NotFound ex) {
                        return false;
                    }
                },
                ex -> {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Restaurant Service is temporarily unavailable",
                            ex
                    );
                }
        );
    }
}
