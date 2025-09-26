package com.richard.paylite.dto;

import com.richard.paylite.model.PaymentStatus;

import java.math.BigDecimal;

public record FullPaymentResponse(
        String paymentId,
        BigDecimal amount,
        String currency,
        String reference,
        PaymentStatus status
) {
}
