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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public PaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request) throws JsonProcessingException, NoSuchAlgorithmException {
        String requestHash = generateRequestHash(request);

        Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingKey.isPresent()) {
            IdempotencyKey key = existingKey.get();
            if (key.getRequestHash().equals(requestHash)) {
                return objectMapper.readValue(key.getResponseBody(), PaymentResponse.class);
            } else {
                throw new ConflictException("Idempotency key used with a different request payload.");
            }
        }

        Payment payment = Payment.builder()
                .paymentId("pl_" + UUID.randomUUID().toString().replace("-", ""))
                .amount(request.amount())
                .currency(request.currency())
                .customerEmail(request.customerEmail())
                .reference(request.reference())
                .status(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);

        PaymentResponse response = new PaymentResponse(payment.getPaymentId(), payment.getStatus().name());
        String responseBody = objectMapper.writeValueAsString(response);

        IdempotencyKey newKey = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey)
                .requestHash(requestHash)
                .responseBody(responseBody)
                .build();
        idempotencyKeyRepository.save(newKey);

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
