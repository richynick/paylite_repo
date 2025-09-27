package com.richard.paylite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record WebhookRequest(
        @Schema(description = "The ID of the payment.", example = "pl_b620d63536284ca0be89982ecb73b5dd")
        @NotBlank @JsonProperty("paymentId") String paymentId,
        @Schema(description = "The type of the event.", example = "payment.succeeded")
        @NotBlank @JsonProperty("event") String event
) {
}
