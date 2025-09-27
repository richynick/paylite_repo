package com.richard.paylite.controller;

import com.richard.paylite.dto.WebhookRequest;
import com.richard.paylite.service.WebhookService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    @Autowired
    private WebhookService webhookService;

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Webhook event payload",
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WebhookRequest.class)
            )
    )
    @PostMapping("/psp")
    public ResponseEntity<Void> handlePspWebhook(
            @RequestHeader("X-PSP-Signature") String signature,
            @Valid @RequestBody String rawPayload) {
        webhookService.processWebhook(signature, rawPayload);
        return ResponseEntity.ok().build();
    }
}
