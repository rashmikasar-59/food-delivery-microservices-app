package com.paymentservice.model;

public class PaymentNotificationEvent {
    private String paymentId;
    private String orderId;
    private String status;
    private String customerUsername;
    public PaymentNotificationEvent() {}
    public PaymentNotificationEvent(String paymentId, String orderId, String status, String customerUsername) {
        this.paymentId = paymentId; this.orderId = orderId; this.status = status; this.customerUsername = customerUsername;
    }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomerUsername() { return customerUsername; }
    public void setCustomerUsername(String customerUsername) { this.customerUsername = customerUsername; }
}
