package com.richard.paylite;

import com.richard.paylite.dto.CreatePaymentRequest;
import com.richard.paylite.dto.FullPaymentResponse;
import com.richard.paylite.dto.PaymentResponse;
import com.richard.paylite.model.PaymentStatus;
import com.richard.paylite.util.SignatureUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PaymentFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SignatureUtil signatureUtil; // Using the real one with the test secret

    @Test
    void testFullPaymentFlow() {
        // === 1. Create a payment intent ===
        String idempotencyKey = UUID.randomUUID().toString();
        CreatePaymentRequest createRequest = new CreatePaymentRequest(
                new BigDecimal("123.45"), "USD", "integration@test.com", "ref-it-1");

        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setContentType(MediaType.APPLICATION_JSON);
        createHeaders.set("X-API-Key", "test-api-key");
        createHeaders.set("Idempotency-Key", idempotencyKey);

        HttpEntity<CreatePaymentRequest> createEntity = new HttpEntity<>(createRequest, createHeaders);

        ResponseEntity<PaymentResponse> createResponse = restTemplate.postForEntity("/api/v1/payments", createEntity, PaymentResponse.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        assertEquals(PaymentStatus.PENDING.name(), createResponse.getBody().status());
        String paymentId = createResponse.getBody().paymentId();
        assertNotNull(paymentId);

        // === 2. Resend the same create request (idempotency check) ===
        ResponseEntity<PaymentResponse> idempotentResponse = restTemplate.postForEntity("/api/v1/payments", createEntity, PaymentResponse.class);
        assertEquals(HttpStatus.OK, idempotentResponse.getStatusCode());
        assertNotNull(idempotentResponse.getBody());
        assertEquals(paymentId, idempotentResponse.getBody().paymentId()); // Should be the same paymentId

        // === 3. Send a webhook to succeed the payment ===
        String webhookPayload = "{\"paymentId\":\"" + paymentId + "\",\"event\":\"payment.succeeded\"}";
        String signature = signatureUtil.calculateHmacSha256(webhookPayload);

        HttpHeaders webhookHeaders = new HttpHeaders();
        webhookHeaders.setContentType(MediaType.APPLICATION_JSON);
        webhookHeaders.set("X-PSP-Signature", signature);

        HttpEntity<String> webhookEntity = new HttpEntity<>(webhookPayload, webhookHeaders);
        ResponseEntity<Void> webhookResponse = restTemplate.postForEntity("/api/v1/webhooks/psp", webhookEntity, Void.class);
        assertEquals(HttpStatus.OK, webhookResponse.getStatusCode());

        // === 4. Get the payment to show its final status is SUCCEEDED ===
        HttpHeaders getHeaders = new HttpHeaders();
        getHeaders.set("X-API-Key", "test-api-key");
        HttpEntity<Void> getEntity = new HttpEntity<>(getHeaders);

        ResponseEntity<FullPaymentResponse> getResponse = restTemplate.exchange(
                "/api/v1/payments/" + paymentId, HttpMethod.GET, getEntity, FullPaymentResponse.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertEquals(PaymentStatus.SUCCEEDED, getResponse.getBody().status());

        // === 5. Deliver a duplicate webhook (should be a no-op) ===
        ResponseEntity<Void> duplicateWebhookResponse = restTemplate.postForEntity("/api/v1/webhooks/psp", webhookEntity, Void.class);
        assertEquals(HttpStatus.OK, duplicateWebhookResponse.getStatusCode());

        // Get payment again to confirm status is still SUCCEEDED and hasn't changed
        ResponseEntity<FullPaymentResponse> finalGetResponse = restTemplate.exchange(
                "/api/v1/payments/" + paymentId, HttpMethod.GET, getEntity, FullPaymentResponse.class);
        assertEquals(PaymentStatus.SUCCEEDED, finalGetResponse.getBody().status());
    }
}
