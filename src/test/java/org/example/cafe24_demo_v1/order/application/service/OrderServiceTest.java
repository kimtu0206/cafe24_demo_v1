package org.example.cafe24_demo_v1.order.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.model.OrderEmbeddedResources;
import org.example.cafe24_demo_v1.order.domain.repository.OrderRepository;
import org.example.cafe24_demo_v1.order.domain.service.Cafe24OrderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository repository;
    @Mock private Cafe24OrderPort cafe24OrderPort;
    @Mock private AppAuthorizationService authorizationService;

    private OrderService orderService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        orderService = new OrderService(repository, cafe24OrderPort, authorizationService);
    }

    @Test
    void syncFromCafe24는_기존_주문이_있으면_갱신한다() {
        LocalDateTime updatedSince = LocalDateTime.now().minusMinutes(10);
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        Order snapshot = order("mymall", "20200717-0029236", "N40", "2000");
        given(cafe24OrderPort.getOrders("mymall", updatedSince, 0, 100, credential)).willReturn(List.of(snapshot));

        Order existing = order("mymall", "20200717-0029236", "N10", "1000");
        given(repository.findByMallIdAndOrderId("mymall", "20200717-0029236")).willReturn(Optional.of(existing));

        orderService.syncFromCafe24("mymall", updatedSince);

        assertThat(existing.getOrderStatus()).isEqualTo("N40");
        assertThat(existing.getTotalAmount()).isEqualTo(new BigDecimal("2000"));
        verify(repository).save(existing);
    }

    @Test
    void syncFromCafe24는_없는_주문이면_신규로_저장한다() {
        LocalDateTime updatedSince = LocalDateTime.now().minusMinutes(10);
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        Order snapshot = order("mymall", "20200717-0029236", "N40", "2000");
        given(cafe24OrderPort.getOrders("mymall", updatedSince, 0, 100, credential)).willReturn(List.of(snapshot));
        given(repository.findByMallIdAndOrderId("mymall", "20200717-0029236")).willReturn(Optional.empty());

        orderService.syncFromCafe24("mymall", updatedSince);

        verify(repository).save(snapshot);
    }

    @Test
    void syncFromCafe24는_페이지가_가득_찰_때까지_반복_조회한다() {
        LocalDateTime updatedSince = LocalDateTime.now().minusMinutes(10);
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        List<Order> fullPage = fixedSizeOrders(100, "P");
        List<Order> lastPage = fixedSizeOrders(20, "Q");

        given(cafe24OrderPort.getOrders("mymall", updatedSince, 0, 100, credential)).willReturn(fullPage);
        given(cafe24OrderPort.getOrders("mymall", updatedSince, 100, 100, credential)).willReturn(lastPage);
        given(repository.findByMallIdAndOrderId(any(), any())).willReturn(Optional.empty());

        orderService.syncFromCafe24("mymall", updatedSince);

        verify(cafe24OrderPort).getOrders("mymall", updatedSince, 0, 100, credential);
        verify(cafe24OrderPort).getOrders("mymall", updatedSince, 100, 100, credential);
        verify(repository, times(120)).save(any());
    }

    @Test
    void upsertFromWebhook은_기존_주문이_있으면_갱신한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Order snapshot = order("mymall", "1", "N40", "2000");
        given(cafe24OrderPort.getOrder("mymall", "1", credential)).willReturn(Optional.of(snapshot));

        Order existing = order("mymall", "1", "N10", "1000");
        given(repository.findByMallIdAndOrderId("mymall", "1")).willReturn(Optional.of(existing));

        orderService.upsertFromWebhook("mymall", "1");

        assertThat(existing.getOrderStatus()).isEqualTo("N40");
        verify(repository).save(existing);
    }

    @Test
    void upsertFromWebhook은_없는_주문이면_신규로_저장한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Order snapshot = order("mymall", "2", "N40", "2000");
        given(cafe24OrderPort.getOrder("mymall", "2", credential)).willReturn(Optional.of(snapshot));
        given(repository.findByMallIdAndOrderId("mymall", "2")).willReturn(Optional.empty());

        orderService.upsertFromWebhook("mymall", "2");

        verify(repository).save(snapshot);
    }

    @Test
    void upsertFromWebhook은_Cafe24에_주문이_없으면_예외를_던진다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24OrderPort.getOrder("mymall", "999", credential)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.upsertFromWebhook("mymall", "999"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void syncFromCafe24는_한_건_처리가_실패해도_나머지_건을_계속_처리한다() {
        LocalDateTime updatedSince = LocalDateTime.now().minusMinutes(10);
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        Order failing = order("mymall", "fail-1", "N10", "1000");
        Order succeeding = order("mymall", "ok-1", "N10", "1000");
        given(cafe24OrderPort.getOrders("mymall", updatedSince, 0, 100, credential))
                .willReturn(List.of(failing, succeeding));
        given(repository.findByMallIdAndOrderId(any(), any())).willReturn(Optional.empty());
        willThrow(new RuntimeException("DB 순단")).given(repository).save(failing);

        orderService.syncFromCafe24("mymall", updatedSince);

        verify(repository).save(succeeding);
    }

    @Test
    void upsertFromWebhook은_동시_삽입_경쟁으로_충돌하면_재조회후_갱신으로_폴백한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Order snapshot = order("mymall", "3", "N40", "2000");
        given(cafe24OrderPort.getOrder("mymall", "3", credential)).willReturn(Optional.of(snapshot));

        Order concurrentlyInserted = order("mymall", "3", "N10", "1000");
        given(repository.findByMallIdAndOrderId("mymall", "3"))
                .willReturn(Optional.empty(), Optional.of(concurrentlyInserted));
        willThrow(new DataIntegrityViolationException("duplicate entry")).given(repository).save(snapshot);

        orderService.upsertFromWebhook("mymall", "3");

        assertThat(concurrentlyInserted.getOrderStatus()).isEqualTo("N40");
        verify(repository).save(concurrentlyInserted);
    }

    private List<Order> fixedSizeOrders(int size, String orderIdPrefix) {
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            orders.add(order("mymall", orderIdPrefix + i, "N10", "1000"));
        }
        return orders;
    }

    private Order order(String mallId, String orderId, String orderStatus, String totalAmount) {
        return Order.register(
                mallId, orderId, orderStatus, "member", "buyer", "buyer@test.com",
                new BigDecimal(totalAmount), "card", LocalDateTime.now(), "{}", OrderEmbeddedResources.empty()
        );
    }
}
