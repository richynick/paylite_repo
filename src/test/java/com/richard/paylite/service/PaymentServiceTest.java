package com.richard.paylite.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Spy // Use @Spy to use a real ObjectMapper that can be tracked by Mockito
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentService paymentService;

    private CreatePaymentRequest createPaymentRequest;

    @BeforeEach
    void setUp() {
        createPaymentRequest = new CreatePaymentRequest(
                new BigDecimal("100.00"),
                "USD",
                "test@example.com",
                "ref-123"
        );
    }

    @Test
    void createPayment_shouldCreateNewPayment_whenKeyIsNew() throws Exception {
        // Given
        String idempotencyKey = "new-key";
        when(idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        PaymentResponse response = paymentService.createPayment(idempotencyKey, createPaymentRequest);

        // Then
        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING.name(), response.status());
        assertNotNull(response.paymentId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(idempotencyKeyRepository, times(1)).save(any(IdempotencyKey.class));
    }

    @Test
    void createPayment_shouldReturnSavedResponse_whenKeyIsSameAndRequestIsSame() throws Exception {
        // Given
        String idempotencyKey = "existing-key";

        // Calculate hash dynamically to ensure it's correct
        String requestJson = objectMapper.writeValueAsString(createPaymentRequest);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(requestJson.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String requestHash = hexString.toString();

        PaymentResponse savedResponse = new PaymentResponse("pl_123", "PENDING");
        String responseJson = objectMapper.writeValueAsString(savedResponse);

        IdempotencyKey key = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey) // Corrected field name
                .requestHash(requestHash)
                .responseBody(responseJson)
                .build();

        when(idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(key));

        // When
        PaymentResponse response = paymentService.createPayment(idempotencyKey, createPaymentRequest);

        // Then
        assertNotNull(response);
        assertEquals("pl_123", response.paymentId());
        verify(paymentRepository, never()).save(any());
        verify(idempotencyKeyRepository, never()).save(any());
    }

    @Test
    void createPayment_shouldThrowConflictException_whenKeyIsSameAndRequestIsDifferent() throws Exception {
        // Given
        String idempotencyKey = "existing-key";
        IdempotencyKey key = IdempotencyKey.builder()
                .idempotencyKey(idempotencyKey) // Corrected field name
                .requestHash("different-hash") // A different hash
                .responseBody("{}")
                .build();

        when(idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(key));

        // When & Then
        assertThrows(ConflictException.class, () -> {
            paymentService.createPayment(idempotencyKey, createPaymentRequest);
        });
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void getPayment_shouldReturnPayment_whenFound() {
        // Given
        String paymentId = "pl_123";
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(PaymentStatus.SUCCEEDED)
                .build();
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(payment));

        // When
        FullPaymentResponse response = paymentService.getPayment(paymentId);

        // Then
        assertNotNull(response);
        assertEquals(paymentId, response.paymentId());
        assertEquals(PaymentStatus.SUCCEEDED, response.status());
    }

    @Test
    void getPayment_shouldThrowNotFoundException_whenNotFound() {
        // Given
        String paymentId = "pl_not_found";
        when(paymentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.getPayment(paymentId);
        });
    }
}
