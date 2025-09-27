package com.richard.paylite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.paylite.dto.WebhookRequest;
import com.richard.paylite.exception.ResourceNotFoundException;
import com.richard.paylite.exception.UnauthorizedException;
import com.richard.paylite.model.Payment;
import com.richard.paylite.model.PaymentStatus;
import com.richard.paylite.repository.PaymentRepository;
import com.richard.paylite.repository.WebhookEventRepository;
import com.richard.paylite.util.SignatureUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private SignatureUtil signatureUtil;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookService webhookService;

    @Test
    void processWebhook_shouldThrowUnauthorizedException_whenSignatureIsInvalid() {
        // Given
        String payload = "payload";
        String signature = "invalid-signature";
        when(signatureUtil.isValidSignature(signature, payload)).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            webhookService.processWebhook(signature, payload);
        });
    }

    @Test
    void processWebhook_shouldDoNothing_whenEventIsAlreadyProcessed() throws Exception {
        // Given
        String payload = "{\"paymentId\":\"pl_123\",\"event\":\"payment.succeeded\"}";
        String signature = "valid-signature";
        WebhookRequest webhookRequest = new WebhookRequest("pl_123", "payment.succeeded");

        when(signatureUtil.isValidSignature(signature, payload)).thenReturn(true);
        when(objectMapper.readValue(payload, WebhookRequest.class)).thenReturn(webhookRequest);
        when(webhookEventRepository.existsByPaymentIdAndEventType("pl_123", "payment.succeeded")).thenReturn(true);

        // When
        webhookService.processWebhook(signature, payload);

        // Then
        verify(paymentRepository, never()).save(any());
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void processWebhook_shouldUpdatePaymentToSucceeded() throws Exception {
        // Given
        String payload = "{\"paymentId\":\"pl_123\",\"event\":\"payment.succeeded\"}";
        String signature = "valid-signature";
        WebhookRequest webhookRequest = new WebhookRequest("pl_123", "payment.succeeded");
        Payment payment = Payment.builder().paymentId("pl_123").status(PaymentStatus.PENDING).build();

        when(signatureUtil.isValidSignature(signature, payload)).thenReturn(true);
        when(objectMapper.readValue(payload, WebhookRequest.class)).thenReturn(webhookRequest);
        when(webhookEventRepository.existsByPaymentIdAndEventType("pl_123", "payment.succeeded")).thenReturn(false);
        when(paymentRepository.findByPaymentId("pl_123")).thenReturn(Optional.of(payment));

        // When
        webhookService.processWebhook(signature, payload);

        // Then
        assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(webhookEventRepository, times(1)).save(any());
    }

    @Test
    void processWebhook_shouldThrowNotFoundException_whenPaymentIsNotFound() throws Exception {
        // Given
        String payload = "{\"paymentId\":\"pl_123\",\"event\":\"payment.succeeded\"}";
        String signature = "valid-signature";
        WebhookRequest webhookRequest = new WebhookRequest("pl_123", "payment.succeeded");

        when(signatureUtil.isValidSignature(signature, payload)).thenReturn(true);
        when(objectMapper.readValue(payload, WebhookRequest.class)).thenReturn(webhookRequest);
        when(webhookEventRepository.existsByPaymentIdAndEventType("pl_123", "payment.succeeded")).thenReturn(false);
        when(paymentRepository.findByPaymentId("pl_123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            webhookService.processWebhook(signature, payload);
        });
    }
}
