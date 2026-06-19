package org.example.cafe24_demo_v1.webhook.presentation;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Optional;

/**
 * 기능별 Webhook 컨트롤러가 공통으로 거치는 검증(서명 확인, 필수값 확인) 로직을 모아둔 베이스 클래스.
 * Cafe24는 200 응답을 받지 못하면 Webhook을 재전송하므로, 검증을 통과하면 각 컨트롤러는 반드시 200을 반환해야 한다.
 */
@Slf4j
abstract class AbstractCafe24WebhookController {

    private final Cafe24WebhookVerifier verifier;

    protected AbstractCafe24WebhookController(Cafe24WebhookVerifier verifier) {
        this.verifier = verifier;
    }

    /** 검증에 실패하면 즉시 반환할 응답을 담아 돌려주고, 통과하면 빈 Optional을 반환한다. */
    protected Optional<ResponseEntity<Void>> reject(Map<String, String> headers, Cafe24WebhookPayload payload) {
        String apiKey = headers.getOrDefault("x-api-key", "");
        if (!verifier.verify(apiKey)) {
            log.warn("Webhook signature verification failed");
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        if (payload.eventNo() == null || payload.resource() == null) {
            log.warn("Webhook payload missing required fields: eventNo={}, resource={}", payload.eventNo(), payload.resource());
            return Optional.of(ResponseEntity.badRequest().build());
        }
        return Optional.empty();
    }

    /**
     * 거의 동시에 들어온 중복 Webhook이 이벤트 이력 테이블의 unique 제약 위반으로 충돌하면 발생한다.
     * 이미 다른 요청이 같은 이벤트를 처리(또는 처리 중)라는 뜻이므로, Cafe24가 재전송하지 않도록 200을 반환한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> handleDuplicateWebhook(DataIntegrityViolationException e) {
        log.info("Duplicate webhook ignored (concurrent race): {}", e.getMessage());
        return ResponseEntity.ok().build();
    }
}
