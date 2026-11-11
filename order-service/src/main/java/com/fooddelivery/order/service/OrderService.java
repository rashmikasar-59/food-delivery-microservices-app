package com.fooddelivery.order.service;

import com.fooddelivery.order.client.MenuServiceClient;
import com.fooddelivery.order.client.RestaurantServiceClient;
import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.MenuItemResponse;
import com.fooddelivery.order.dto.OrderItemRequest;
import com.fooddelivery.order.model.Order;
import com.fooddelivery.order.model.OrderItem;
import com.fooddelivery.order.model.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuServiceClient menuServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;

    public OrderService(
            OrderRepository orderRepository,
            MenuServiceClient menuServiceClient,
            RestaurantServiceClient restaurantServiceClient) {

        this.orderRepository = orderRepository;
        this.menuServiceClient = menuServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
    }

    public Order createOrder(String customerEmail, CreateOrderRequest request) {

        Order order = new Order();

        order.setCustomerId(customerEmail);
        order.setRestaurantId(request.getRestaurantId());
        order.setDeliveryAddress(request.getDeliveryAddress());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {

            MenuItemResponse menuItem =
                    menuServiceClient.getMenuItem(itemRequest.getMenuItemId());

            if (menuItem == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Menu item not found");
            }

            if (!request.getRestaurantId().equals(
                    menuItem.getRestaurantId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Menu item does not belong to the selected restaurant");
            }

            if (!menuItem.isAvailable()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Menu item is currently unavailable: "
                                + menuItem.getName());
            }

            if (menuItem.getPrice() < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid menu item price");
            }

            BigDecimal price = BigDecimal.valueOf(menuItem.getPrice());

            BigDecimal subtotal = price.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = new OrderItem();

            orderItem.setMenuItemId(menuItem.getId());
            orderItem.setName(menuItem.getName());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setPrice(price);
            orderItem.setSubtotal(subtotal);

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.PLACED);

        LocalDateTime now = LocalDateTime.now();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        return orderRepository.save(order);
    }

    public List<Order> getOrdersByCustomer(String customerEmail) {
        return orderRepository.findByCustomerId(customerEmail);
    }

    public Order getOrderById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found: " + orderId));
    }

    public Order getOrderForCustomer(
            String orderId, String customerEmail) {

        Order order = getOrderById(orderId);

        if (!order.getCustomerId().equals(customerEmail)) {
            throw new AccessDeniedException(
                    "You cannot access another customer's order");
        }

        return order;
    }

    public Order updateOrderStatus(
            String orderId,
            OrderStatus status,
            String currentUser,
            Collection<? extends GrantedAuthority> authorities) {

        Order order = getOrderById(orderId);

        boolean isAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            boolean isRestaurantOwner = authorities.stream()
                    .anyMatch(a -> a.getAuthority()
                            .equals("ROLE_RESTAURANT_OWNER"));

            if (!isRestaurantOwner) {
                throw new AccessDeniedException(
                        "Only restaurant owners or admins can update order status");
            }

            verifyRestaurantOwnership(order.getRestaurantId(), currentUser);
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    private void verifyRestaurantOwnership(
            String restaurantId, String currentUser) {

        if (!restaurantServiceClient.isRestaurantOwner(
                restaurantId, currentUser)) {
            throw new AccessDeniedException(
                    "You are not authorized to manage this restaurant's orders");
        }
    }

    public void cancelOrder(String orderId, String customerEmail) {

        Order order = getOrderForCustomer(orderId, customerEmail);

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order cannot be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        orderRepository.save(order);
    }
}