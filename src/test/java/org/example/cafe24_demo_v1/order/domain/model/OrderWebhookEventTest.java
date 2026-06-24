package org.example.cafe24_demo_v1.order.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderWebhookEventTest {

    @Test
    void receive로_생성하면_RECEIVED_상태로_초기화된다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive(
                "mymall", 90023, "ORDER_CREATED", "20200717-0029236", null, "{\"order_id\":\"20200717-0029236\"}"
        );

        assertThat(event.getMallId()).isEqualTo("mymall");
        assertThat(event.getEventNo()).isEqualTo(90023);
        assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
        assertThat(event.getResourceId()).isEqualTo("20200717-0029236");
        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.RECEIVED);
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getReceivedAt()).isNotNull();
    }

    @Test
    void markProcessed으로_처리완료_상태가_되고_재시도_예약이_해제된다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        event.markFailed("일시 실패");

        event.markProcessed();

        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.PROCESSED);
        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getNextRetryAt()).isNull();
        assertThat(event.getErrorMessage()).isNull();
    }

    @Test
    void markFailed으로_실패_사유가_기록되고_재시도_시각이_미래로_설정된다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");

        event.markFailed("Cafe24 API 호출 실패");

        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.FAILED);
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getErrorMessage()).isEqualTo("Cafe24 API 호출 실패");
        assertThat(event.getNextRetryAt()).isAfter(java.time.LocalDateTime.now());
    }

    @Test
    void markFailed이_MAX_RETRY_COUNT번_누적되면_DEAD로_전환된다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");

        for (int i = 0; i < 4; i++) {
            event.markFailed("실패 " + i);
            assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.FAILED);
        }
        event.markFailed("5번째 실패");

        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.DEAD);
    }
}
