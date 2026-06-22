package org.example.cafe24_demo_v1.order.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.repository.OrderWebhookEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * 미처리 이벤트를 Cafe24에서 다시 조회해 order 테이블에 반영한다.
     * OrderWebhookEventProcessor가 주기적으로 호출한다. 실패해도 processed가 false로 남아 다음 주기에 재시도된다.
     *
     * 스케줄 주기가 하루 1회로 길어, 한 번 실행에 미처리 건이 배치 크기(PROCESS_BATCH_SIZE)를 넘게
     * 쌓여 있어도 모두 처리될 때까지 배치 단위로 반복 조회한다.
     */
    public void processUnprocessed() {
        List<OrderWebhookEvent> events;
        do {
            events = repository.findUnprocessed(PROCESS_BATCH_SIZE);
            events.forEach(this::process);
        } while (events.size() == PROCESS_BATCH_SIZE);
    }

    private void process(OrderWebhookEvent event) {
        try {
            orderService.upsertFromWebhook(event.getMallId(), event.getResourceId());
            event.markProcessed();
        } catch (Exception e) {
            log.error("Order webhook event 처리 실패: mallId={}, orderId={}", event.getMallId(), event.getResourceId(), e);
            event.markFailed(e.getMessage());
        }
        repository.save(event);
    }
}
