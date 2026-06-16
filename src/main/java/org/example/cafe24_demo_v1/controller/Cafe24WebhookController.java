package org.example.cafe24_demo_v1.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.dto.Cafe24WebhookRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class Cafe24WebhookController {

    @Value("${cafe24.webhook.api-key}")
    private String webhookApiKey;

    @PostMapping("/cafe24")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody(required = false) Cafe24WebhookRequest request
    ) {

        log.info("Cafe24 Webhook Received");

        if (!webhookApiKey.equals(apiKey)) {
            log.warn("Invalid API Key : {}", apiKey);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (request == null) {
            log.info("Webhook Body Empty");
            return ResponseEntity.ok().build();
        }

        log.info("eventNo={}", request.getEventNo());
        log.info("resource={}", request.getResource());

        return ResponseEntity.ok().build();
    }
}
