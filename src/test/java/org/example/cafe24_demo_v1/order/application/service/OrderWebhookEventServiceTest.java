package org.example.cafe24_demo_v1.order.application.service;

import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.repository.OrderWebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderWebhookEventServiceTest {

    @Mock private OrderWebhookEventRepository repository;
    @Mock private OrderService orderService;

    private OrderWebhookEventService service;

    @BeforeEach
    void setUp() {
        service = new OrderWebhookEventService(repository, orderService);
    }

    @Test
    void saveRaw는_신규_이벤트면_저장한다() {
        given(repository.exists(90023, "mymall", "1")).willReturn(false);

        service.saveRaw("mymall", 90023, "1", "{}");

        verify(repository).save(any(OrderWebhookEvent.class));
    }

    @Test
    void saveRaw는_이미_처리한_이벤트면_무시한다() {
        given(repository.exists(90023, "mymall", "1")).willReturn(true);

        service.saveRaw("mymall", 90023, "1", "{}");

        verify(repository, never()).save(any());
    }

    @Test
    void processUnprocessed은_성공하면_처리완료로_표시한다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        given(repository.findUnprocessed(50)).willReturn(List.of(event));

        service.processUnprocessed();

        verify(orderService).upsertFromWebhook("mymall", "1");
        assertThat(event.isProcessed()).isTrue();
        verify(repository).save(event);
    }

    @Test
    void processUnprocessed은_실패하면_에러메시지를_남기고_미처리로_둔다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        given(repository.findUnprocessed(50)).willReturn(List.of(event));
        willThrow(new IllegalStateException("주문을 찾을 수 없음")).given(orderService).upsertFromWebhook("mymall", "1");

        service.processUnprocessed();

        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getErrorMessage()).isEqualTo("주문을 찾을 수 없음");
        verify(repository).save(event);
    }
}
