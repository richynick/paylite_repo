package com.richard.paylite.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.richard.paylite.dto.CreatePaymentRequest;
import com.richard.paylite.dto.FullPaymentResponse;
import com.richard.paylite.dto.PaymentResponse;
import com.richard.paylite.service.PaymentService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPaymentIntent(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) throws NoSuchAlgorithmException, JsonProcessingException {
        logger.info("Received request to create payment intent");
        PaymentResponse response = paymentService.createPayment(idempotencyKey, request);
        logger.info("Successfully created payment intent with id: {}", response.paymentId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<FullPaymentResponse> getPayment(
            @Parameter(description = "The ID of the payment.", example = "pl_b620d63536284ca0be89982ecb73b5dd")
            @PathVariable String paymentId) {
        logger.info("Received request to get payment with id: {}", paymentId);
        FullPaymentResponse response = paymentService.getPayment(paymentId);
        logger.info("Successfully retrieved payment with id: {}", paymentId);
        return ResponseEntity.ok(response);
    }
}
