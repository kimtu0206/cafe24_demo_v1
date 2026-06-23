package org.example.cafe24_demo_v1.webhook.presentation;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierCreatedEvent;
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
@Slf4j
@RestController
@RequestMapping("/webhook/cafe24/carriers")
public class CarrierWebhookController extends AbstractCafe24WebhookController {

    private final ApplicationEventPublisher eventPublisher;

    public CarrierWebhookController(Cafe24WebhookVerifier verifier, ApplicationEventPublisher eventPublisher) {
        super(verifier);
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/created")
    public ResponseEntity<Void> created(@RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {

        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireShippingCarrierCode(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, shippingCarrierCode={}", payload.eventNo(), payload.resource().shippingCarrierCode());
            eventPublisher.publishEvent(
                    new CarrierCreatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().shippingCarrierCode())
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
