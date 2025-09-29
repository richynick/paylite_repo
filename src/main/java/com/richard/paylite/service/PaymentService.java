package com.richard.paylite.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.paylite.dto.CreatePaymentRequest;
import com.richard.paylite.dto.FullPaymentResponse;
import com.richard.paylite.dto.PaymentResponse;
import com.richard.paylite.exception.ConflictException;
import com.richard.paylite.exception.ResourceNotFoundException;
import com.richard.paylite.model.IdempotencyKey;
import com.richard.paylite.model.Payment;
import com.richard.paylite.model.PaymentStatus;
import com.richard.paylite.repository.IdempotencyKeyRepository;
import com.richard.paylite.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request) throws JsonProcessingException, NoSuchAlgorithmException {
        logger.info("Processing payment creation with idempotency key: {}", idempotencyKey);
        String requestHash = generateRequestHash(request);

        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();
            if (key.getRequestHash().equals(requestHash)) {
                logger.info("Idempotency key hit. Returning cached response for key: {}", idempotencyKey);
                return objectMapper.readValue(key.getResponseBody(), PaymentResponse.class);
            } else {
                logger.warn("Idempotency key conflict for key: {}", idempotencyKey);
                throw new ConflictException("Idempotency key used with a different request payload.");
            }
        }

        logger.info("Creating new payment...");
        Payment payment = Payment.builder()
                .paymentId("pl_" + UUID.randomUUID().toString().replace("-", ""))
                .amount(request.amount())
                .currency(request.currency())
                .customerEmail(request.customerEmail())
                .reference(request.reference())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);
        logger.info("Successfully saved new payment with id: {}", payment.getPaymentId());

        PaymentResponse response = new PaymentResponse(payment.getPaymentId(), payment.getStatus().name());
        String responseBody = objectMapper.writeValueAsString(response);

        IdempotencyKey newKey = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .responseBody(responseBody)
                .build();
        idempotencyKeyRepository.save(newKey);
        logger.info("Saved new idempotency key: {}", idempotencyKey);

        return response;
    }

    public FullPaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        return new FullPaymentResponse(
                payment.getPaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus()
        );
    }

    private String generateRequestHash(CreatePaymentRequest request) throws JsonProcessingException, NoSuchAlgorithmException {
        String requestJson = objectMapper.writeValueAsString(request);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(requestJson.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
