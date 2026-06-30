package org.example.cafe24_demo_v1.webhook.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.domain.event.BenefitCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.BenefitUpdatedEvent;
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
 * Cafe24 혜택 등록 Webhook 수신 컨트롤러.
 * Cafe24 개발자센터에서 /webhook/cafe24/benefits/created를 해당 이벤트 전용 Webhook URL로 등록해야 한다.
 */
@Tag(name = "Webhook")
@SecurityRequirement(name = "WebhookApiKey")
@Slf4j
@RestController
@RequestMapping("/webhook/cafe24/benefits")
public class BenefitWebhookController extends AbstractCafe24WebhookController {

    private final ApplicationEventPublisher eventPublisher;

    public BenefitWebhookController(Cafe24WebhookVerifier verifier, ApplicationEventPublisher eventPublisher) {
        super(verifier);
        this.eventPublisher = eventPublisher;
    }

    @Operation(summary = "혜택 등록 Webhook 수신", description = "Cafe24 혜택 등록 이벤트를 수신하고 Cafe24 API로 재조회해 로컬 DB에 반영합니다.")
    @PostMapping("/created")
    public ResponseEntity<Void> created(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {
        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireBenefitNo(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, benefitNo={}", payload.eventNo(), payload.resource().benefitNo());
            eventPublisher.publishEvent(
                    new BenefitCreatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().benefitNo())
            );
            return ResponseEntity.ok().build();
        });
    }

    @Operation(summary = "혜택 수정 Webhook 수신", description = "Cafe24 혜택 수정 이벤트를 수신하고 Cafe24 API로 재조회해 로컬 DB에 반영합니다.")
    @PostMapping("/updated")
    public ResponseEntity<Void> updated(@Parameter(hidden = true) @RequestHeader Map<String, String> headers, @RequestBody Cafe24WebhookPayload payload) {
        return reject(headers, payload.eventNo(), payload.resource()).or(() -> requireBenefitNo(payload)).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, benefitNo={}", payload.eventNo(), payload.resource().benefitNo());
            eventPublisher.publishEvent(
                    new BenefitUpdatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().benefitNo())
            );
            return ResponseEntity.ok().build();
        });
    }

    private Optional<ResponseEntity<Void>> requireBenefitNo(Cafe24WebhookPayload payload) {
        if (payload.resource().benefitNo() == null) {
            log.warn("Webhook payload missing benefitNo: eventNo={}", payload.eventNo());
            return Optional.of(ResponseEntity.badRequest().build());
        }
        return Optional.empty();
    }
}
