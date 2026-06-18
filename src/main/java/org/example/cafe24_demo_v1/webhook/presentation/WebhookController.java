package org.example.cafe24_demo_v1.webhook.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.webhook.domain.event.AppUninstalledEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductCreatedEvent;
import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

/**
 * Cafe24가 앱 이벤트를 Push 방식으로 전달하는 Webhook 수신 컨트롤러.
 *
 * 역할을 최소화해 다음 두 가지만 처리한다:
 * 1. 서명 검증 (Cafe24WebhookVerifier)
 * 2. 도메인 이벤트 발행 (ApplicationEventPublisher)
 *
 * 실제 비즈니스 처리는 이벤트를 구독하는 WebhookEventService가 담당한다.
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    // Cafe24 앱 삭제 이벤트 번호 (Cafe24 공식 문서 기준)
    private static final int EVENT_APP_UNINSTALLED = 10;

    private final Cafe24WebhookVerifier verifier;
    private final ApplicationEventPublisher eventPublisher;
    private final Cafe24Properties cafe24Properties;

    /**
     * Cafe24 Webhook 수신 엔드포인트.
     * 서명 검증 실패 시 401을 반환하고, 성공 시 이벤트를 발행한 뒤 200을 반환한다.
     * Cafe24는 200 응답을 받지 못하면 Webhook을 재전송하므로 반드시 200을 반환해야 한다.
     */
    @PostMapping("/cafe24")
    public ResponseEntity<Void> receive(
            @RequestHeader Map<String, String> headers,
            @RequestBody WebhookPayload payload) {

        // 1. x-api-key 헤더로 Cafe24에서 보낸 요청인지 검증
        String apiKey = headers.getOrDefault("x-api-key", "");
        if (!verifier.verify(apiKey)) {
            log.warn("Webhook signature verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. 필수값 검증
        if (payload.eventNo() == null || payload.resource() == null) {
            log.warn("Webhook payload missing required fields: eventNo={}, resource={}", payload.eventNo(), payload.resource());
            return ResponseEntity.badRequest().build();
        }

        log.info("Webhook received: eventNo={}", payload.eventNo());
        handleEvent(payload);

        return ResponseEntity.ok().build();
    }

    /** 이벤트 번호에 따라 적절한 도메인 이벤트를 발행한다. */
    private void handleEvent(WebhookPayload payload) {
        if (payload.resource() == null) {
            return;
        }
        if (payload.eventNo() == EVENT_APP_UNINSTALLED) {
            eventPublisher.publishEvent(
                    new AppUninstalledEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().clientId())
            );
        } else if (isProductCreatedEvent(payload.eventNo())) {
            eventPublisher.publishEvent(
                    new ProductCreatedEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().productNo())
            );
        }
    }

    /** 상품 생성 이벤트 번호는 Cafe24 앱 설정에 따라 달라지므로 application.yml에서 읽는다. */
    private boolean isProductCreatedEvent(Integer eventNo) {
        Integer productCreatedEventNo = cafe24Properties.getWebhook().getProductCreatedEventNo();
        return productCreatedEventNo != null && Objects.equals(productCreatedEventNo, eventNo);
    }

    /** Cafe24 Webhook 요청 바디 구조 */
    private record WebhookPayload(
            @JsonProperty("event_no") Integer eventNo,
            Resource resource
    ) {}

    /** 이벤트가 발생한 쇼핑몰과 리소스 정보 */
    private record Resource(
            @JsonProperty("mall_id") String mallId,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("app_name") String appName,
            @JsonProperty("deleted_date") String deletedDate, // 앱 삭제 이벤트의 경우 삭제 일시
            @JsonProperty("product_no") Long productNo        // 상품 생성 이벤트의 경우 상품 번호
    ) {}
}
