package org.example.cafe24_demo_v1.admin.application;

import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncRunStatus;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock private OrderWebhookEventService orderWebhookEventService;
    @Mock private SyncMetricsService syncMetricsService;

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(orderWebhookEventService, syncMetricsService);
    }

    @Test
    void getMetrics는_webhook_지표와_동기화_상태를_하나로_합친다() {
        OrderWebhookEventService.WebhookMetrics webhookMetrics =
                new OrderWebhookEventService.WebhookMetrics(3, 1, 0, 5);
        given(orderWebhookEventService.getMetrics()).willReturn(webhookMetrics);

        SyncRunStatus orderStatus = SyncRunStatus.init("mymall", SyncTarget.ORDER);
        given(syncMetricsService.getAll("mymall")).willReturn(List.of(orderStatus));

        MetricsService.OperationalMetrics result = metricsService.getMetrics("mymall");

        assertThat(result.webhook()).isSameAs(webhookMetrics);
        assertThat(result.syncStatuses()).containsExactly(orderStatus);
    }
}
