package com.richard.paylite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @Schema(description = "The amount of the payment.", example = "1999")
        @NotNull @Positive BigDecimal amount,
        @Schema(description = "The currency of the payment.", example = "NGN")
        @NotBlank String currency,
        @Schema(description = "The email address of the customer.", example = "user@example.com")
        @NotBlank @Email String customerEmail,
        @Schema(description = "A reference for the payment.", example = "INV-2025-0001")
        String reference
) {
}
