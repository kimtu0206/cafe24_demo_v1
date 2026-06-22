package org.example.cafe24_demo_v1.order.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 하루(24시간)에 한 번 미처리(processed=false) 주문 Webhook 이벤트를 Cafe24 재조회로 처리하는 스케줄러.
 * Webhook 응답 자체는 원본 저장만으로 빠르게 끝내고, 실제 order 테이블 반영은 이 스케줄러가 비동기로 담당한다.
 */
@Component
@RequiredArgsConstructor
public class OrderWebhookEventProcessor {

    private static final long PROCESS_INTERVAL_HOURS = 24;

    private final OrderWebhookEventService orderWebhookEventService;

    @Scheduled(fixedRate = PROCESS_INTERVAL_HOURS * 60 * 60 * 1000L)
    public void process() {
        orderWebhookEventService.processUnprocessed();
    }
}
