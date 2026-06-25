package org.example.cafe24_demo_v1.admin.application;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncRunStatus;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 운영 관측용 지표(Webhook 처리 현황 + 동기화 실행 현황)를 한 번에 조회하기 위해
 * order/monitoring 컨텍스트의 조회 메서드를 모아 응답을 구성하는 애플리케이션 서비스.
 */
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final OrderWebhookEventService orderWebhookEventService;
    private final SyncMetricsService syncMetricsService;

    public OperationalMetrics getMetrics(String mallId) {
        OrderWebhookEventService.WebhookMetrics webhook = orderWebhookEventService.getMetrics();
        List<SyncRunStatus> syncStatuses = syncMetricsService.getAll(mallId);
        return new OperationalMetrics(webhook, syncStatuses);
    }

    public record OperationalMetrics(
            OrderWebhookEventService.WebhookMetrics webhook,
            List<SyncRunStatus> syncStatuses
    ) {}
}
