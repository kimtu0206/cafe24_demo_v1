package org.example.cafe24_demo_v1.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.repository.OrderWebhookEventRepository;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 Webhook의 원본 데이터 저장과 비동기 처리(재시도 포함)를 담당하는 애플리케이션 서비스.
 *
 * Webhook 수신 시 saveRaw로 원본만 저장하고 즉시 끝내며, 실제 order 테이블 반영은
 * processUnprocessed가 별도 스케줄러(OrderWebhookEventProcessor)에 의해 비동기로 수행한다.
 * 이렇게 분리하면 Cafe24 응답 대기시간이 Webhook 응답 속도에 영향을 주지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderWebhookEventService {

    private static final int PROCESS_BATCH_SIZE = 50;
    private static final String ORDER_CREATED_EVENT_TYPE = "ORDER_CREATED";

    private final OrderWebhookEventRepository repository;
    private final OrderService orderService;

    /**
     * Webhook 원본 payload를 저장한다. (eventNo, mallId, orderId) 조합으로 중복 수신을 확인해
     * 최초 수신 시에만 저장한다. Cafe24 재조회나 order 테이블 반영은 하지 않는다.
     */
    @Transactional
    public void saveRaw(String mallId, Integer eventNo, String orderId, String payload) {
        if (repository.exists(eventNo, mallId, orderId)) {
            log.info("Duplicate order webhook ignored: eventNo={}, mallId={}, orderId={}", eventNo, mallId, orderId);
            return;
        }
        repository.save(OrderWebhookEvent.receive(mallId, eventNo, ORDER_CREATED_EVENT_TYPE, orderId, null, payload));
    }

    /**
     * 재시도 대상 이벤트를 Cafe24에서 다시 조회해 order 테이블에 반영한다.
     * OrderWebhookEventProcessor가 짧은 주기로 반복 호출한다.
     * 실패한 이벤트는 nextRetryAt만큼 미뤄지므로 같은 실행 안에서 바로 재조회되지 않는다.
     *
     * 한 번 실행에 재시도 대상이 배치 크기(PROCESS_BATCH_SIZE)를 넘게 쌓여 있어도
     * 모두 처리될 때까지 배치 단위로 반복 조회한다.
     */
    public void processUnprocessed() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime processingStaleBefore = now.minus(OrderWebhookEvent.PROCESSING_STALE_TIMEOUT);
        List<OrderWebhookEvent> events;
        do {
            events = repository.findRetryableEvents(now, processingStaleBefore, PROCESS_BATCH_SIZE);
            events.forEach(this::process);
        } while (events.size() == PROCESS_BATCH_SIZE);
    }

    /**
     * Cafe24 호출 직전에 PROCESSING으로 한 번 저장해 시도 시작을 기록한 뒤(크래시 시 복구 기준),
     * 호출 결과에 따라 최종 상태로 다시 저장한다.
     *
     * Cafe24ApiException이 400(Bad Request)이면 재시도해도 결과가 달라지지 않는 영구 오류로 보고
     * 즉시 DEAD 처리한다. 401/403/429/5xx는 토큰 재인증 문제나 Cafe24 측 일시적 가용성 문제일 수
     * 있어 영구 오류로 단정하지 않고, "주문을 찾을 수 없음" 같은 그 외 모든 예외와 함께 기존
     * 재시도(backoff) 경로로 처리한다.
     */
    private void process(OrderWebhookEvent event) {
        event.markProcessing();
        repository.save(event);
        try {
            orderService.upsertFromWebhook(event.getMallId(), event.getResourceId());
            event.markProcessed();
        } catch (Exception e) {
            log.error("Order webhook event 처리 실패: mallId={}, orderId={}", event.getMallId(), event.getResourceId(), e);
            if (isPermanentError(e)) {
                event.markDead(e.getMessage());
            } else {
                event.markFailed(e.getMessage());
            }
        }
        repository.save(event);
    }

    private boolean isPermanentError(Exception e) {
        return e instanceof Cafe24ApiException cafe24Exception
                && cafe24Exception.getStatusCode() != null
                && cafe24Exception.getStatusCode().value() == 400;
    }
}
