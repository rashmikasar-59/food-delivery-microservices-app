package com.fooddelivery.menu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.menu.dto.CreateMenuItemRequest;
import com.fooddelivery.menu.dto.UpdateMenuItemRequest;
import com.fooddelivery.menu.entity.MenuItem;
import com.fooddelivery.menu.exception.GlobalExceptionHandler;
import com.fooddelivery.menu.exception.ResourceNotFoundException;
import com.fooddelivery.menu.service.MenuItemService;
import com.fooddelivery.menu.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MenuItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MenuItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MenuItemService menuItemService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UsernamePasswordAuthenticationToken authentication(
            String username, String role) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    private CreateMenuItemRequest createRequest() {
        CreateMenuItemRequest request = new CreateMenuItemRequest();
        request.setRestaurantId("restaurant-1");
        request.setName("Paneer Pizza");
        request.setDescription("Fresh paneer pizza");
        request.setPrice(249.0);
        request.setCategory("Pizza");
        request.setImageUrl("pizza.jpg");
        request.setVegetarian(true);
        return request;
    }

    private UpdateMenuItemRequest updateRequest() {
        UpdateMenuItemRequest request = new UpdateMenuItemRequest();
        request.setName("Updated Pizza");
        request.setDescription("Updated description");
        request.setPrice(299.0);
        request.setCategory("Special Pizza");
        request.setImageUrl("updated.jpg");
        request.setVegetarian(true);
        request.setAvailable(true);
        return request;
    }

    @Test
    void createMenuItemShouldReturnCreatedForRestaurantOwner() throws Exception {
        MenuItem item = new MenuItem(
                "restaurant-1", "Paneer Pizza", "Fresh paneer pizza",
                249.0, "Pizza", "pizza.jpg", true);
        item.setId("item-1");

        when(menuItemService.createMenuItem(
                any(CreateMenuItemRequest.class), eq("owner-1"), eq(false)))
                .thenReturn(item);

        mockMvc.perform(post("/api/v1/menu-items")
                        .principal(authentication("owner-1", "ROLE_RESTAURANT_OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("item-1"))
                .andExpect(jsonPath("$.name").value("Paneer Pizza"));

        verify(menuItemService).createMenuItem(
                any(CreateMenuItemRequest.class), eq("owner-1"), eq(false));
    }

    @Test
    void createMenuItemShouldDetectAdminRole() throws Exception {
        MenuItem item = new MenuItem(
                "restaurant-1", "Paneer Pizza", "Fresh paneer pizza",
                249.0, "Pizza", null, true);

        when(menuItemService.createMenuItem(
                any(CreateMenuItemRequest.class), eq("admin-1"), eq(true)))
                .thenReturn(item);

        mockMvc.perform(post("/api/v1/menu-items")
                        .principal(authentication("admin-1", "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());

        verify(menuItemService).createMenuItem(
                any(CreateMenuItemRequest.class), eq("admin-1"), eq(true));
    }

    @Test
    void createMenuItemShouldReturnBadRequestForInvalidInput() throws Exception {
        CreateMenuItemRequest request = new CreateMenuItemRequest();

        mockMvc.perform(post("/api/v1/menu-items")
                        .principal(authentication(
                                "owner-1", "ROLE_RESTAURANT_OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(menuItemService);
    }

    @Test
    void getRestaurantMenuShouldReturnItems() throws Exception {
        MenuItem item = new MenuItem(
                "restaurant-1", "Pizza", "Cheese pizza",
                199.0, "Pizza", null, true);

        when(menuItemService.getRestaurantMenu("restaurant-1"))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/menu-items/restaurant/restaurant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pizza"));

        verify(menuItemService).getRestaurantMenu("restaurant-1");
    }

    @Test
    void getMenuItemShouldReturnItemById() throws Exception {
        MenuItem item = new MenuItem(
                "restaurant-1", "Pizza", "Cheese pizza",
                199.0, "Pizza", null, true);
        item.setId("item-1");

        when(menuItemService.getMenuItemById("item-1"))
                .thenReturn(item);

        mockMvc.perform(get("/api/v1/menu-items/item-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("item-1"))
                .andExpect(jsonPath("$.name").value("Pizza"));
    }

    @Test
    void getMenuItemShouldReturnNotFoundWhenItemDoesNotExist() throws Exception {
        when(menuItemService.getMenuItemById("missing-item"))
                .thenThrow(new ResourceNotFoundException(
                        "Menu item not found: missing-item"));

        mockMvc.perform(get("/api/v1/menu-items/missing-item"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Menu item not found: missing-item"));
    }

    @Test
    void updateMenuItemShouldReturnUpdatedItem() throws Exception {
        MenuItem item = new MenuItem(
                "restaurant-1", "Updated Pizza", "Updated description",
                299.0, "Special Pizza", "updated.jpg", true);
        item.setId("item-1");

        when(menuItemService.updateMenuItem(
                eq("item-1"), any(UpdateMenuItemRequest.class),
                eq("owner-1"), eq(false)))
                .thenReturn(item);

        mockMvc.perform(put("/api/v1/menu-items/item-1")
                        .principal(authentication(
                                "owner-1", "ROLE_RESTAURANT_OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Pizza"))
                .andExpect(jsonPath("$.price").value(299.0));

        verify(menuItemService).updateMenuItem(
                eq("item-1"), any(UpdateMenuItemRequest.class),
                eq("owner-1"), eq(false));
    }

    @Test
    void deleteMenuItemShouldReturnNoContentForAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/menu-items/item-1")
                        .principal(authentication("admin-1", "ROLE_ADMIN")))
                .andExpect(status().isNoContent());

        verify(menuItemService).deleteMenuItem("item-1", "admin-1", true);
    }
}
