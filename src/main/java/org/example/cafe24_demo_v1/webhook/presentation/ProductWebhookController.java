package org.example.cafe24_demo_v1.webhook.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductUpdatedEvent;
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
 * Cafe24 상품 생성/수정/삭제 Webhook 수신 컨트롤러.
 * Cafe24 개발자센터에서 각 엔드포인트를 해당 이벤트 전용 Webhook URL로 등록해야 한다.
 */
@Tag(name = "Webhook")
@SecurityRequirement(name = "WebhookApiKey")
@Slf4j
@RestController
@RequestMapping("/webhook/cafe24/products")
public class ProductWebhookController extends AbstractCafe24WebhookController {

    private final ApplicationEventPublisher eventPublisher;

    public ProductWebhookController(Cafe24WebhookVerifier verifier, ApplicationEventPublisher eventPublisher) {
        super(verifier);
        this.eventPublisher = eventPublisher;
    }

    @Operation(summary = "상품 생성 Webhook 수신", description = "Cafe24 상품 생성 이벤트를 수신하고 Cafe24 API로 재조회해 로컬 DB에 반영합니다.")
    @PostMapping("/created")
    public ResponseEntity<Void> created(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {
        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireProductNo(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, productNo={}", payload.eventNo(), payload.resource().productNo());
            eventPublisher.publishEvent(
                    new ProductCreatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().productNo())
            );
            return ResponseEntity.ok().build();
        });
    }

    @Operation(summary = "상품 수정 Webhook 수신", description = "Cafe24 상품 수정 이벤트를 수신하고 Cafe24 API로 재조회해 로컬 DB를 갱신합니다.")
    @PostMapping("/updated")
    public ResponseEntity<Void> updated(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {
        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireProductNo(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, productNo={}", payload.eventNo(), payload.resource().productNo());
            eventPublisher.publishEvent(
                    new ProductUpdatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().productNo())
            );
            return ResponseEntity.ok().build();
        });
    }

    @Operation(summary = "상품 삭제 Webhook 수신", description = "Cafe24 상품 삭제 이벤트를 수신하고 로컬 DB에서 해당 상품을 삭제합니다.")
    @PostMapping("/deleted")
    public ResponseEntity<Void> deleted(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {
        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireProductNo(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, productNo={}", payload.eventNo(), payload.resource().productNo());
            eventPublisher.publishEvent(
                    new ProductDeletedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().productNo())
            );
            return ResponseEntity.ok().build();
        });
    }

    /** 상품 Webhook은 공통 검증(reject)과 별개로 productNo가 반드시 있어야 한다. */
    private Optional<ResponseEntity<Void>> requireProductNo(Cafe24WebhookPayload payload) {
        if (payload.resource().productNo() == null) {
            log.warn("Webhook payload missing productNo: eventNo={}", payload.eventNo());
            return Optional.of(ResponseEntity.badRequest().build());
        }
        return Optional.empty();
    }
}
