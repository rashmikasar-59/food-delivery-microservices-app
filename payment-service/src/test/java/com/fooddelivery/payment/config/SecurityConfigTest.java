package com.fooddelivery.payment.config;

import com.fooddelivery.payment.security.JwtService;
import com.fooddelivery.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldRejectUnauthenticatedPaymentRequests() throws Exception {
        mockMvc.perform(get("/api/payments/customer"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer")
    void shouldAllowAuthenticatedPaymentRequests() throws Exception {
        when(paymentService.getPaymentsByCustomer("customer"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/payments/customer"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublicHealthEndpoint() throws Exception {
        int responseStatus = mockMvc.perform(get("/actuator/health"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertNotEquals(401, responseStatus);
        assertNotEquals(403, responseStatus);
    }

    @Test
    void shouldAllowPublicSwaggerEndpoint() throws Exception {
        int responseStatus = mockMvc.perform(get("/v3/api-docs"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertNotEquals(401, responseStatus);
        assertNotEquals(403, responseStatus);
    }
}
