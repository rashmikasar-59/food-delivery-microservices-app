package com.fooddelivery.restaurant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void shouldConfigureApiInformation() {
        OpenAPI openAPI = config.restaurantOpenAPI();

        assertNotNull(openAPI.getInfo());
        assertEquals("Restaurant Service API", openAPI.getInfo().getTitle());
        assertEquals(
                "Food Delivery Platform - Restaurant Management",
                openAPI.getInfo().getDescription()
        );
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
    }

    @Test
    void shouldConfigureBearerJwtSecurityScheme() {
        OpenAPI openAPI = config.restaurantOpenAPI();

        SecurityScheme scheme = openAPI.getComponents()
                .getSecuritySchemes()
                .get("bearerAuth");

        assertNotNull(scheme);
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
        assertEquals("bearerAuth", scheme.getName());
    }

    @Test
    void shouldAddBearerAuthenticationRequirement() {
        OpenAPI openAPI = config.restaurantOpenAPI();

        assertNotNull(openAPI.getSecurity());
        assertEquals(1, openAPI.getSecurity().size());
        assertTrue(openAPI.getSecurity().get(0).containsKey("bearerAuth"));
    }
}
