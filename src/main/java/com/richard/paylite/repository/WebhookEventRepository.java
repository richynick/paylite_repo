package com.richard.paylite.repository;

import com.richard.paylite.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByPaymentIdAndEventType(String paymentId, String eventType);
}
