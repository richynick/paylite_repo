package com.richard.paylite.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.paylite.dto.WebhookRequest;
import com.richard.paylite.exception.ResourceNotFoundException;
import com.richard.paylite.exception.UnauthorizedException;
import com.richard.paylite.model.Payment;
import com.richard.paylite.model.PaymentStatus;
import com.richard.paylite.model.WebhookEvent;
import com.richard.paylite.repository.PaymentRepository;
import com.richard.paylite.repository.WebhookEventRepository;
import com.richard.paylite.util.SignatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    @Autowired
    private SignatureUtil signatureUtil;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public void processWebhook(String signature, String rawPayload) {
        logger.info("Received webhook with signature: {}", signature);
        logger.info("Raw webhook payload: {}", rawPayload);

        if (!signatureUtil.isValidSignature(signature, rawPayload)) {
            throw new UnauthorizedException("Invalid webhook signature.");
        }

        try {
            WebhookRequest request = objectMapper.readValue(rawPayload, WebhookRequest.class);

            if (webhookEventRepository.existsByPaymentIdAndEventType(request.paymentId(), request.event())) {
                // Event already processed, return 200 OK
                return;
            }

            Payment payment = paymentRepository.findByPaymentId(request.paymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found for webhook: " + request.paymentId()));

            if (payment.getStatus() == PaymentStatus.PENDING) {
                switch (request.event()) {
                    case "payment.succeeded":
                        payment.setStatus(PaymentStatus.SUCCEEDED);
                        break;
                    case "payment.failed":
                        payment.setStatus(PaymentStatus.FAILED);
                        break;
                    default:
                        // Or throw a bad request exception
                        return;
                }
                paymentRepository.save(payment);
            }

            WebhookEvent event = WebhookEvent.builder()
                    .paymentId(request.paymentId())
                    .eventType(request.event())
                    .rawPayload(rawPayload)
                    .build();
            webhookEventRepository.save(event);

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing webhook payload", e);
        }
    }
}
