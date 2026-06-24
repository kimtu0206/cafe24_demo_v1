package org.example.cafe24_demo_v1.order.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 재시도 대상 주문 Webhook 이벤트를 Cafe24 재조회로 처리하는 워커.
 * Webhook 수신 시점에는 원본 저장만으로 빠르게 끝내고, 실제 order 테이블 반영은
 * 이 워커가 짧은 주기로 비동기 처리한다 — Webhook을 "즉시 반영"의 트리거로 쓰기 위함.
 * Webhook 누락 보정을 위한 장주기 전체 재조회는 별도의 OrderSyncScheduler가 담당한다.
 *
 * fixedRate가 아닌 fixedDelay를 쓰는 이유: processUnprocessed()가 배치를 모두 소진할 때까지
 * 반복 조회하므로, 처리 시간이 길어져도 이전 실행이 끝난 뒤에만 다음 실행이 시작되도록 한다.
 */
@Component
@RequiredArgsConstructor
public class OrderWebhookEventProcessor {

    private static final long PROCESS_DELAY_MS = 60 * 1000L;

    private final OrderWebhookEventService orderWebhookEventService;

    @Scheduled(fixedDelay = PROCESS_DELAY_MS)
    public void process() {
        orderWebhookEventService.processUnprocessed();
    }
}
