package com.fooddelivery.order.client;

import com.fooddelivery.order.dto.MenuItemResponse;
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

@Component
public class MenuServiceClient {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public MenuServiceClient(
            RestClient.Builder builder,
            @Value("${menu-service.base-url}") String baseUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        this.circuitBreaker = circuitBreakerFactory.create("orderMenuService");
    }

    public MenuItemResponse getMenuItem(String menuItemId) {
        return circuitBreaker.run(
                () -> {
                    try {
                        return restClient.get()
                                .uri("/api/v1/menu-items/{id}", menuItemId)
                                .retrieve()
                                .body(MenuItemResponse.class);
                    } catch (HttpClientErrorException.NotFound ex) {
                        return null;
                    }
                },
                ex -> {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "Menu Service is temporarily unavailable",
                            ex
                    );
                }
        );
    }
}
