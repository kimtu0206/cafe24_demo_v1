package org.example.cafe24_demo_v1.webhook.presentation;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

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

    /**
     * 검증에 실패하면 즉시 반환할 응답을 담아 돌려주고, 통과하면 빈 Optional을 반환한다.
     * resource는 Webhook 종류마다 타입이 달라(record 또는 JsonNode) Object로 받아 null 여부만 확인한다.
     */
    protected Optional<ResponseEntity<Void>> reject(Map<String, String> headers, Integer eventNo, Object resource) {
        String apiKey = headers.getOrDefault("x-api-key", "");
        if (!verifier.verify(apiKey)) {
            log.warn("Webhook signature verification failed");
            return Optional.of(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }
        if (eventNo == null || resource == null) {
            log.warn("Webhook payload missing required fields: eventNo={}, resource={}", eventNo, resource);
            return Optional.of(ResponseEntity.badRequest().build());
        }
        return Optional.empty();
    }

    /**
     * 공통 검증(reject) 통과 후 기능별 추가 검증(extraValidation)까지 통과하면 이벤트를 발행하고 200을,
     * 검증에 실패하면 그 결과(401/400)를 반환한다. 생성/수정/삭제 Webhook 엔드포인트마다 반복되던
     * "검증 → 이벤트 발행 → 200 응답" 조합을 통합한다.
     */
    protected ResponseEntity<Void> handle(Map<String, String> headers, Integer eventNo, Object resource,
                                           Supplier<Optional<ResponseEntity<Void>>> extraValidation, Runnable publishEvent) {
        return reject(headers, eventNo, resource).or(extraValidation).orElseGet(() -> {
            publishEvent.run();
            return ResponseEntity.ok().build();
        });
    }

    /**
     * 거의 동시에 들어온 중복 Webhook이 이벤트 이력 테이블의 unique 제약 위반으로 충돌하면 발생한다.
     * Hibernate가 Spring Data JPA를 통해 unique 제약 위반을 던지면 DataIntegrityViolationException으로
     * 변환되어 올라온다(JdbcTemplate 전용인 DuplicateKeyException은 이 경로로 던져지지 않는다).
     * 이미 다른 요청이 같은 이벤트를 처리(또는 처리 중)라는 뜻이므로, Cafe24가 재전송하지 않도록 200을 반환한다.
     * unique 제약이 아닌 다른 무결성 위반(NOT NULL 등)은 실제 데이터 오류이므로 여기서 삼키지 않고
     * 그대로 전파시켜 Cafe24가 재전송하도록 한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> handleDuplicateWebhook(DataIntegrityViolationException e) {
        if (!isUniqueConstraintViolation(e)) {
            throw e;
        }
        log.info("Duplicate webhook ignored (concurrent race): {}", e.getMessage());
        return ResponseEntity.ok().build();
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cve
                && cve.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE;
    }
}
