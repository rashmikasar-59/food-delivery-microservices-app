package com.fooddelivery.order.service;

import com.fooddelivery.order.client.MenuServiceClient;
import com.fooddelivery.order.client.RestaurantServiceClient;
import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.MenuItemResponse;
import com.fooddelivery.order.dto.OrderItemRequest;
import com.fooddelivery.order.model.Order;
import com.fooddelivery.order.model.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuServiceClient menuServiceClient;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    private OrderService orderService;

    private static final String CUSTOMER = "customer@example.com";
    private static final String RESTAURANT = "restaurant-1";
    private static final String ORDER_ID = "order-1";

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                menuServiceClient,
                restaurantServiceClient
        );
    }

    private CreateOrderRequest createRequest(String restaurantId, String menuItemId, int quantity) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setRestaurantId(restaurantId);
        request.setDeliveryAddress("Pune, Maharashtra");

        OrderItemRequest item = new OrderItemRequest();
        item.setMenuItemId(menuItemId);
        item.setQuantity(quantity);

        request.setItems(List.of(item));
        return request;
    }

    private MenuItemResponse menuItem(String restaurantId, double price, boolean available) {
        MenuItemResponse item = new MenuItemResponse();
        item.setId("menu-1");
        item.setRestaurantId(restaurantId);
        item.setName("Paneer Pizza");
        item.setPrice(price);
        item.setAvailable(available);
        return item;
    }

    private Order existingOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER);
        order.setRestaurantId(RESTAURANT);
        order.setStatus(OrderStatus.PLACED);
        return order;
    }

    private Collection<SimpleGrantedAuthority> authorities(String... roles) {
        return java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    @Test
    void createOrderShouldCalculateSubtotalAndTotal() {
        CreateOrderRequest request = createRequest(RESTAURANT, "menu-1", 2);
        MenuItemResponse item = menuItem(RESTAURANT, 249.0, true);

        when(menuServiceClient.getMenuItem("menu-1")).thenReturn(item);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.createOrder(CUSTOMER, request);

        assertEquals(CUSTOMER, result.getCustomerId());
        assertEquals(RESTAURANT, result.getRestaurantId());
        assertEquals(new BigDecimal("498.0"), result.getTotalAmount());
        assertEquals(OrderStatus.PLACED, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(new BigDecimal("498.0"), result.getItems().get(0).getSubtotal());
        assertNotNull(result.getCreatedAt());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrderShouldRejectMissingMenuItem() {
        CreateOrderRequest request = createRequest(RESTAURANT, "missing", 1);

        when(menuServiceClient.getMenuItem("missing")).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(CUSTOMER, request));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderShouldRejectItemFromAnotherRestaurant() {
        CreateOrderRequest request = createRequest(RESTAURANT, "menu-1", 1);

        when(menuServiceClient.getMenuItem("menu-1"))
                .thenReturn(menuItem("another-restaurant", 100.0, true));

        assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(CUSTOMER, request));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderShouldRejectUnavailableMenuItem() {
        CreateOrderRequest request = createRequest(RESTAURANT, "menu-1", 1);

        when(menuServiceClient.getMenuItem("menu-1"))
                .thenReturn(menuItem(RESTAURANT, 100.0, false));

        assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(CUSTOMER, request));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrderShouldRejectNegativeMenuItemPrice() {
        CreateOrderRequest request = createRequest(RESTAURANT, "menu-1", 1);

        when(menuServiceClient.getMenuItem("menu-1"))
                .thenReturn(menuItem(RESTAURANT, -10.0, true));

        assertThrows(ResponseStatusException.class,
                () -> orderService.createOrder(CUSTOMER, request));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrdersByCustomerShouldReturnCustomerOrders() {
        List<Order> orders = List.of(existingOrder());
        when(orderRepository.findByCustomerId(CUSTOMER)).thenReturn(orders);

        assertEquals(orders, orderService.getOrdersByCustomer(CUSTOMER));
    }

    @Test
    void getOrderByIdShouldReturnOrder() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertEquals(order, orderService.getOrderById(ORDER_ID));
    }

    @Test
    void getOrderByIdShouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> orderService.getOrderById(ORDER_ID));
    }

    @Test
    void getOrderForCustomerShouldRejectAnotherCustomer() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.getOrderForCustomer(ORDER_ID, "other@example.com"));
    }

    @Test
    void updateOrderStatusShouldAllowAdmin() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.updateOrderStatus(
                ORDER_ID,
                OrderStatus.CONFIRMED,
                "admin@example.com",
                authorities("ROLE_ADMIN")
        );

        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatusShouldAllowRestaurantOwner() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        when(restaurantServiceClient.isRestaurantOwner(RESTAURANT, "owner@example.com")).thenReturn(true);

        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.updateOrderStatus(
                ORDER_ID,
                OrderStatus.PREPARING,
                "owner@example.com",
                authorities("ROLE_RESTAURANT_OWNER")
        );

        assertEquals(OrderStatus.PREPARING, result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatusShouldRejectCustomer() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.updateOrderStatus(
                        ORDER_ID,
                        OrderStatus.CONFIRMED,
                        CUSTOMER,
                        authorities("ROLE_CUSTOMER")
                ));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateOrderStatusShouldRejectDifferentRestaurantOwner() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        when(restaurantServiceClient.isRestaurantOwner(
                RESTAURANT, "owner@example.com")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> orderService.updateOrderStatus(
                        ORDER_ID,
                        OrderStatus.CONFIRMED,
                        "owner@example.com",
                        authorities("ROLE_RESTAURANT_OWNER")
                ));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderShouldAllowOrderOwner() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        orderService.cancelOrder(ORDER_ID, CUSTOMER);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderShouldRejectAnotherCustomer() {
        Order order = existingOrder();
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class,
                () -> orderService.cancelOrder(ORDER_ID, "other@example.com"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderShouldRejectDeliveredOrder() {
        Order order = existingOrder();
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(ORDER_ID, CUSTOMER));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderShouldRejectAlreadyCancelledOrder() {
        Order order = existingOrder();
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThrows(ResponseStatusException.class,
                () -> orderService.cancelOrder(ORDER_ID, CUSTOMER));

        verify(orderRepository, never()).save(any());
    }
}
