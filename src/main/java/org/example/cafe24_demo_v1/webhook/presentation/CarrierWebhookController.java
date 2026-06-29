package org.example.cafe24_demo_v1.webhook.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierUpdatedEvent;
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
 * Cafe24 배송사 등록 Webhook 수신 컨트롤러.
 * Cafe24 개발자센터에서 이 엔드포인트를 배송사 등록 이벤트 전용 Webhook URL로 등록해야 한다.
 */
@Tag(name = "Webhook")
@SecurityRequirement(name = "WebhookApiKey")
@Slf4j
@RestController
@RequestMapping("/webhook/cafe24/carriers")
public class CarrierWebhookController extends AbstractCafe24WebhookController {

    private final ApplicationEventPublisher eventPublisher;

    public CarrierWebhookController(Cafe24WebhookVerifier verifier, ApplicationEventPublisher eventPublisher) {
        super(verifier);
        this.eventPublisher = eventPublisher;
    }

    @Operation(summary = "배송사 등록 Webhook 수신", description = "Cafe24 배송사 등록 이벤트를 수신합니다. 실제 DB 반영은 CarrierSyncScheduler 주기 동기화가 담당합니다.")
    @PostMapping("/created")
    public ResponseEntity<Void> created(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {

        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireShippingCarrierCode(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, shippingCarrierCode={}", payload.eventNo(), payload.resource().shippingCarrierCode());
            eventPublisher.publishEvent(
                    new CarrierCreatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().shippingCarrierCode())
            );
            return ResponseEntity.ok().build();
        });
    }

    @Operation(summary = "배송사 수정 Webhook 수신", description = "Cafe24 배송사 수정 이벤트를 수신합니다. 실제 DB 반영은 CarrierSyncScheduler 주기 동기화가 담당합니다.")
    @PostMapping("/updated")
    public ResponseEntity<Void> updated(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {
        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireShippingCarrierCode(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, shippingCarrierCode={}", payload.eventNo(), payload.resource().shippingCarrierCode());
            eventPublisher.publishEvent(
                    new CarrierUpdatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().shippingCarrierCode())
            );
            return ResponseEntity.ok().build();
        });
    }

    @Operation(summary = "배송사 삭제 Webhook 수신", description = "Cafe24 배송사 삭제 이벤트를 수신합니다. 실제 DB 반영은 CarrierSyncScheduler 주기 동기화가 담당합니다.")
    @PostMapping("/deleted")
    public ResponseEntity<Void> deleted(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {
        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireShippingCarrierCode(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, shippingCarrierCode={}", payload.eventNo(), payload.resource().shippingCarrierCode());
            eventPublisher.publishEvent(
                    new CarrierDeletedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().shippingCarrierCode())
            );
            return ResponseEntity.ok().build();
        });
    }

    /** 배송사 Webhook은 공통 검증(reject)과 별개로 shippingCarrierCode가 반드시 있어야 한다. */
    private Optional<ResponseEntity<Void>> requireShippingCarrierCode(Cafe24WebhookPayload payload) {
        if (payload.resource().shippingCarrierCode() == null) {
            log.warn("Webhook payload missing shippingCarrierCode: eventNo={}", payload.eventNo());
            return Optional.of(ResponseEntity.badRequest().build());
        }
        return Optional.empty();
    }
}
