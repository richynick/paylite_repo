package com.richard.paylite.dto;

import com.richard.paylite.model.PaymentStatus;

public record PaymentResponse(
        String paymentId,
        String status
) {
}
