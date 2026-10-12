package com.deliveryservice.service;

import com.deliveryservice.model.DeliveryAgent;
import com.deliveryservice.model.DeliveryAssignment;
import com.deliveryservice.model.OrderDetails;
import com.deliveryservice.model.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

@Service
@RequiredArgsConstructor
public class DeliveryService {
    @Autowired
    private WebClient.Builder webClientBuilder;
    public String deliverOrder(String orderId, String username, String authorization) {
        // update order status to delivered
        webClientBuilder.build().put()
                .uri("http://order-service/api/v1/order/status",
                        uriBuilder -> uriBuilder
                                .queryParam("orderId", orderId)
                                .build())
                .header("loggedInRole", "DELIVERY_AGENT")
                .header("loggedInUser", username)
                .header("Authorization", authorization)
                .bodyValue(OrderStatus.DELIVERED)
                .retrieve()
                .toBodilessEntity()
                .block();
        return "Order with id: " + orderId + " delivered";
    }

    // This method creates a new delivery assignment after receiving a delivery request from
    // order-service
    
     public void receiveOrderAssignment() {
        createNewAssignment(new OrderDetails());
     }
     private void createNewAssignment(OrderDetails orderDetails) {
        // search for a delivery Agent - talk to auth service for that
        DeliveryAgent[] deliveryAgents = webClientBuilder.build().get()
                .uri("http://auth-service/api/v1/user/delivery",
                        UriBuilder::build)
                .retrieve()
                .bodyToMono(DeliveryAgent[].class)
                .block();
        
        if (deliveryAgents == null || deliveryAgents.length == 0) {
            throw new IllegalStateException("No delivery agents are currently available");
        }
        // TODO: sort these agents based on the distance between their current location and restaurant location.
         DeliveryAssignment deliveryAssignment = DeliveryAssignment.builder()
                 .deliveryAgentName(deliveryAgents[0].getName())
                 .deliveryAgentCurrentAddress(deliveryAgents[0].getAddress())
                 .deliveryAgentPhoneNumber(deliveryAgents[0].getPhoneNumber())
                 .orderDetails(orderDetails)
                 .build();
         
         // TODO: send a notification to deliveryAgent that an order is assigned
         // TODO: if agent accepts the assignment track his location
    }

    public void callCustomer() {
    }
}
