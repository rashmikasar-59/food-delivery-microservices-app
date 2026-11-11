package com.fooddelivery.restaurant.config;

import com.fooddelivery.restaurant.security.JwtAuthenticationFilter;
import com.fooddelivery.restaurant.security.JwtService;
import com.fooddelivery.restaurant.security.RestAccessDeniedHandler;
import com.fooddelivery.restaurant.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAccessDeniedHandler.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private JwtService jwtService;

    private static final String BASE_URL = "/api/v1/restaurants";

    private static final String CREATE_JSON = """
            {
              "name": "Test Restaurant",
              "description": "Test description",
              "email": "test@example.com",
              "phone": "9876543210",
              "address": "Test Address",
              "city": "Pune",
              "cuisine": "Indian"
            }
            """;

    private static final String UPDATE_JSON = """
            {
              "name": "Updated Restaurant",
              "description": "Updated description",
              "phone": "9876543210",
              "address": "Updated Address",
              "city": "Pune",
              "cuisine": "Indian"
            }
            """;

    @Test
    void shouldAllowPublicRestaurantListing() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnauthenticatedRestaurantCreation() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectUserRoleForRestaurantCreation() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void shouldAllowRestaurantOwnerToCreateRestaurant() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToCreateRestaurant() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectUserRoleForRestaurantUpdate() throws Exception {
        mockMvc.perform(put(BASE_URL + "/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void shouldAllowRestaurantOwnerToUpdateRestaurant() throws Exception {
        mockMvc.perform(put(BASE_URL + "/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "RESTAURANT_OWNER")
    void shouldAllowRestaurantOwnerToDeleteRestaurant() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/123"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectUnauthenticatedRestaurantDeletion() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRequireAuthenticationForOtherEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/private"))
                .andExpect(status().isForbidden());
    }
}
