package org.example.cafe24_demo_v1.webhook.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.domain.event.OrderCancelledEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.OrderCreatedEvent;
import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Cafe24 주문 생성 Webhook 수신 컨트롤러.
 * Cafe24 개발자센터에서 이 엔드포인트를 주문 생성 이벤트 전용 Webhook URL로 등록해야 한다.
 *
 * 다른 Webhook과 달리 여기서는 Cafe24 재조회나 order 테이블 반영을 하지 않는다.
 * 원본 payload 저장만 위임하고 즉시 200을 반환해, 응답 시간이 Cafe24/DB 상태에 영향받지 않게 한다.
 * 실제 반영은 OrderWebhookEventProcessor가 비동기로 처리한다.
 */
@Tag(name = "Webhook")
@SecurityRequirement(name = "WebhookApiKey")
@Slf4j
@RestController
@RequestMapping("/webhook/cafe24/orders")
public class OrderWebhookController extends AbstractCafe24WebhookController {

    private final ApplicationEventPublisher eventPublisher;

    public OrderWebhookController(Cafe24WebhookVerifier verifier, ApplicationEventPublisher eventPublisher) {
        super(verifier);
        this.eventPublisher = eventPublisher;
    }

    @Operation(summary = "주문 생성 Webhook 수신", description = "Cafe24 주문 생성 이벤트를 수신하고 원본 payload만 저장합니다. 실제 주문 반영은 OrderWebhookEventProcessor가 비동기로 처리합니다.")
    @PostMapping("/created")
    public ResponseEntity<Void> created(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody OrderWebhookPayload payload) {
        JsonNode resource = payload.resource();
        Object resourceOrNull = (resource == null || resource.isNull()) ? null : resource;

        return handle(headers, payload.eventNo(), resourceOrNull, () -> requireOrderId(payload), () -> {
            log.info("Webhook received: eventNo={}, orderId={}", payload.eventNo(), payload.orderId());
            eventPublisher.publishEvent(
                    new OrderCreatedEvent(payload.eventNo(), payload.mallId(), payload.orderId(), resource.toString())
            );
        });
    }

    @Operation(summary = "주문 취소 상태 변경 Webhook 수신", description = "Cafe24 주문 취소 상태 변경 이벤트를 수신하고 원본 payload만 저장합니다. 실제 주문 반영은 OrderWebhookEventProcessor가 비동기로 처리합니다.")
    @PostMapping("/cancelled")
    public ResponseEntity<Void> cancelled(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody OrderWebhookPayload payload) {
        JsonNode resource = payload.resource();
        Object resourceOrNull = (resource == null || resource.isNull()) ? null : resource;

        return handle(headers, payload.eventNo(), resourceOrNull, () -> requireOrderId(payload), () -> {
            log.info("Webhook received: eventNo={}, orderId={}", payload.eventNo(), payload.orderId());
            eventPublisher.publishEvent(
                    new OrderCancelledEvent(payload.eventNo(), payload.mallId(), payload.orderId(), resource.toString())
            );
        });
    }

    /** 주문 Webhook은 공통 검증(reject)과 별개로 order_id가 반드시 있어야 한다. */
    private Optional<ResponseEntity<Void>> requireOrderId(OrderWebhookPayload payload) {
        if (payload.orderId() == null) {
            log.warn("Webhook payload missing order_id: eventNo={}", payload.eventNo());
            return Optional.of(ResponseEntity.badRequest().build());
        }
        return Optional.empty();
    }
}
