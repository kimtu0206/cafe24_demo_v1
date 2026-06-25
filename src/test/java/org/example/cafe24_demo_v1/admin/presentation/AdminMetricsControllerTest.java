package org.example.cafe24_demo_v1.admin.presentation;

import org.example.cafe24_demo_v1.admin.application.MetricsService;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminMetricsControllerTest {

    @Mock private MetricsService metricsService;

    private AdminMetricsController controller;

    @BeforeEach
    void setUp() {
        Cafe24Properties properties = new Cafe24Properties();
        properties.setMallId("mymall");
        controller = new AdminMetricsController(metricsService, properties);
    }

    @Test
    void getMetrics는_Cafe24Properties의_mallId로_MetricsService를_호출하고_200을_반환한다() {
        MetricsService.OperationalMetrics metrics = new MetricsService.OperationalMetrics(
                new OrderWebhookEventService.WebhookMetrics(0, 0, 0, 0), List.of()
        );
        given(metricsService.getMetrics("mymall")).willReturn(metrics);

        ResponseEntity<MetricsService.OperationalMetrics> response = controller.getMetrics();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(metrics);
    }
}
