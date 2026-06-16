package org.example.cafe24_demo_v1.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.dto.Cafe24WebhookRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class Cafe24WebhookController {

    private final Cafe24Properties cafe24Properties;

    @PostMapping("/cafe24")
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            @RequestBody(required = false) Cafe24WebhookRequest request
    ) {
        log.info("Cafe24 Webhook Received");

        if (!cafe24Properties.getWebhook().getApiKey().equals(apiKey)) {
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
