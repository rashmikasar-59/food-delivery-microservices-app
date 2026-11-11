package com.fooddelivery.menu.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerStateTest {

    @Test
    void shouldTransitionFromClosedToOpenWhenFailureThresholdIsReached() {
        CircuitBreaker breaker = createCircuitBreaker();

        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());

        assertThrows(RuntimeException.class,
                () -> breaker.executeSupplier(() -> {
                    throw new RuntimeException("Service unavailable");
                }));

        assertThrows(RuntimeException.class,
                () -> breaker.executeSupplier(() -> {
                    throw new RuntimeException("Service unavailable");
                }));

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        assertThrows(CallNotPermittedException.class,
                () -> breaker.executeSupplier(() -> "response"));
    }

    @Test
    void shouldTransitionFromHalfOpenToClosedAfterSuccessfulTrialCall() {
        CircuitBreaker breaker = createCircuitBreaker();

        for (int i = 0; i < 2; i++) {
            assertThrows(RuntimeException.class,
                    () -> breaker.executeSupplier(() -> {
                        throw new RuntimeException("Service unavailable");
                    }));
        }

        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());

        breaker.transitionToHalfOpenState();

        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.getState());

        String response = breaker.executeSupplier(() -> "service recovered");

        assertEquals("service recovered", response);
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }

    private CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .permittedNumberOfCallsInHalfOpenState(1)
                .build();

        return CircuitBreakerRegistry.of(config)
                .circuitBreaker("testCircuitBreaker");
    }
}
