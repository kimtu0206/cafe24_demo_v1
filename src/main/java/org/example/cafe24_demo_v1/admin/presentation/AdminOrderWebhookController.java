package org.example.cafe24_demo_v1.admin.presentation;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/retry")
@RequiredArgsConstructor
public class AdminOrderWebhookController {

    private final OrderWebhookEventService orderWebhookEventService;

    @PostMapping("/order-webhooks/{id}")
    public ResponseEntity<String> retryDead(@PathVariable Long id) {
        try {
            orderWebhookEventService.retryDead(id);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
