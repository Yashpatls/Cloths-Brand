package com.stylehub.dto;

import java.math.BigDecimal;

public class RazorpayOrderResponse {
    private String razorpayOrderId;
    private String keyId;
    private BigDecimal amount;
    private String currency;
    private String orderNumber;

    public RazorpayOrderResponse(String razorpayOrderId, String keyId, BigDecimal amount, String currency, String orderNumber) {
        this.razorpayOrderId = razorpayOrderId;
        this.keyId = keyId;
        this.amount = amount;
        this.currency = currency;
        this.orderNumber = orderNumber;
    }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public String getKeyId() { return keyId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getOrderNumber() { return orderNumber; }
}
