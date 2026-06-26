package org.example.cafe24_demo_v1.order.application.service;

import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEvent;
import org.example.cafe24_demo_v1.order.domain.model.OrderWebhookEventStatus;
import org.example.cafe24_demo_v1.order.domain.repository.OrderWebhookEventRepository;
import org.example.cafe24_demo_v1.shared.config.WorkerProperties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderWebhookEventServiceTest {

    @Mock private OrderWebhookEventRepository repository;
    @Mock private OrderService orderService;

    private OrderWebhookEventService service;

    @BeforeEach
    void setUp() {
        WorkerProperties workerProperties = new WorkerProperties();
        workerProperties.getOrderWebhook().setBatchSize(50);
        workerProperties.getOrderWebhook().setMaxRetryCount(5);
        service = new OrderWebhookEventService(repository, orderService, workerProperties);
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
        given(repository.findRetryableEvents(any(), any(), eq(50))).willReturn(List.of(event));

        service.processUnprocessed();

        verify(orderService).upsertFromWebhook("mymall", "1");
        assertThat(event.isProcessed()).isTrue();
        verify(repository, times(2)).save(event);
    }

    @Test
    void processUnprocessed은_시도_직전에_PROCESSING으로_먼저_저장한다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        given(repository.findRetryableEvents(any(), any(), eq(50))).willReturn(List.of(event));

        service.processUnprocessed();

        assertThat(event.getLastTriedAt()).isNotNull();
    }

    @Test
    void processUnprocessed은_실패하면_에러메시지를_남기고_재시도상태로_둔다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        given(repository.findRetryableEvents(any(), any(), eq(50))).willReturn(List.of(event));
        willThrow(new IllegalStateException("주문을 찾을 수 없음")).given(orderService).upsertFromWebhook("mymall", "1");

        service.processUnprocessed();

        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.FAILED);
        assertThat(event.getErrorMessage()).isEqualTo("주문을 찾을 수 없음");
        verify(repository, times(2)).save(event);
    }

    @Test
    void processUnprocessed은_Cafe24가_400을_반환하면_재시도없이_즉시_DEAD로_전환한다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        given(repository.findRetryableEvents(any(), any(), eq(50))).willReturn(List.of(event));
        willThrow(new Cafe24ApiException("Cafe24 order API call failed. status=400", HttpStatus.BAD_REQUEST, "{}", null))
                .given(orderService).upsertFromWebhook("mymall", "1");

        service.processUnprocessed();

        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.DEAD);
        assertThat(event.getNextRetryAt()).isNull();
        verify(repository, times(2)).save(event);
    }

    @Test
    void processUnprocessed은_Cafe24가_5xx를_반환하면_기존_재시도_경로로_처리한다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        given(repository.findRetryableEvents(any(), any(), eq(50))).willReturn(List.of(event));
        willThrow(new Cafe24ApiException("Cafe24 order API call failed. status=500", HttpStatus.INTERNAL_SERVER_ERROR, "{}", null))
                .given(orderService).upsertFromWebhook("mymall", "1");

        service.processUnprocessed();

        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.FAILED);
        assertThat(event.getNextRetryAt()).isNotNull();
    }

    @Test
    void getMetrics는_상태별_건수와_재시도_합계를_집계한다() {
        given(repository.countByStatus(OrderWebhookEventStatus.RECEIVED)).willReturn(3L);
        given(repository.countByStatus(OrderWebhookEventStatus.PROCESSING)).willReturn(1L);
        given(repository.countByStatus(OrderWebhookEventStatus.FAILED)).willReturn(2L);
        given(repository.countByStatus(OrderWebhookEventStatus.DEAD)).willReturn(4L);
        given(repository.sumRetryCount()).willReturn(15L);

        OrderWebhookEventService.WebhookMetrics metrics = service.getMetrics();

        assertThat(metrics.unprocessedCount()).isEqualTo(6L);
        assertThat(metrics.failedCount()).isEqualTo(2L);
        assertThat(metrics.deadCount()).isEqualTo(4L);
        assertThat(metrics.totalRetryCount()).isEqualTo(15L);
    }

    @Test
    void retryDead는_DEAD_이벤트를_RECEIVED로_초기화하고_저장한다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        event.markDead("영구 오류");
        given(repository.findById(1L)).willReturn(Optional.of(event));

        service.retryDead(1L);

        assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.RECEIVED);
        assertThat(event.getRetryCount()).isZero();
        verify(repository).save(event);
    }

    @Test
    void retryDead는_이벤트가_없으면_NoSuchElementException을_던진다() {
        given(repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.retryDead(999L))
                .isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void retryDead는_DEAD가_아닌_이벤트면_IllegalStateException을_던진다() {
        OrderWebhookEvent event = OrderWebhookEvent.receive("mymall", 90023, "ORDER_CREATED", "1", null, "{}");
        event.markFailed("일시 오류", 5);
        given(repository.findById(1L)).willReturn(Optional.of(event));

        assertThatThrownBy(() -> service.retryDead(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void processUnprocessed은_같은_실행안에서_실패건을_무한정_재조회하지_않는다() {
        List<OrderWebhookEvent> fullBatch = java.util.stream.IntStream.range(0, 50)
                .mapToObj(i -> OrderWebhookEvent.receive("mymall", 90023 + i, "ORDER_CREATED", String.valueOf(i), null, "{}"))
                .toList();
        willThrow(new IllegalStateException("Cafe24 호출 실패")).given(orderService).upsertFromWebhook(any(), any());
        given(repository.findRetryableEvents(any(), any(), eq(50)))
                .willReturn(fullBatch)
                .willReturn(List.of());

        service.processUnprocessed();

        verify(repository, times(2)).findRetryableEvents(any(), any(), eq(50));
        fullBatch.forEach(event -> assertThat(event.getStatus()).isEqualTo(OrderWebhookEventStatus.FAILED));
    }
}
