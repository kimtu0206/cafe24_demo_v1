package org.example.cafe24_demo_v1.order.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderWebhookEventTest {

    @Test
    void receive로_생성하면_미처리_상태로_초기화된다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive(
                "mymall", 90023, "ORDER_CREATED", "20200717-0029236", null, "{\"order_id\":\"20200717-0029236\"}"
        );

        assertThat(event.getMallId()).isEqualTo("mymall");
        assertThat(event.getEventNo()).isEqualTo(90023);
        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getResourceId()).isEqualTo("20200717-0029236");
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getReceivedAt()).isNotNull();
    }

    @Test
    void markProcessed으로_처리완료_상태가_된다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");

        event.markProcessed();

        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getErrorMessage()).isNull();
    }

    @Test
    void markFailed으로_실패_사유가_기록되고_미처리_상태로_남는다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");

        event.markFailed("Cafe24 API 호출 실패");

        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo("Cafe24 API 호출 실패");
    }
}
