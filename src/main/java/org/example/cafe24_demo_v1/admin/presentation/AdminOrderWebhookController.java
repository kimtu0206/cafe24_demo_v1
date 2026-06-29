package org.example.cafe24_demo_v1.admin.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@Tag(name = "Admin")
@RestController
@RequestMapping("/admin/retry")
@RequiredArgsConstructor
public class AdminOrderWebhookController {

    private final OrderWebhookEventService orderWebhookEventService;

    @Operation(summary = "DEAD 주문 Webhook 재시도", description = "DEAD 상태인 주문 Webhook 이벤트를 RECEIVED로 초기화합니다. 다음 워커 주기에 자동으로 재처리됩니다.")
    @PostMapping("/order-webhooks/{id}")
    public ResponseEntity<String> retryDead(@Parameter(description = "재시도할 Webhook 이벤트 ID") @PathVariable Long id) {
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
