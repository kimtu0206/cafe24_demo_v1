package org.example.cafe24_demo_v1.admin.application;

import org.example.cafe24_demo_v1.carrier.application.service.CarrierService;
import org.example.cafe24_demo_v1.order.application.service.OrderService;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BackfillServiceTest {

    @Mock private OrderService orderService;
    @Mock private ProductService productService;
    @Mock private CarrierService carrierService;

    private BackfillService backfillService;

    @BeforeEach
    void setUp() {
        backfillService = new BackfillService(orderService, productService, carrierService);
    }

    @Test
    void backfillOrders는_OrderService의_결과를_그대로_반환한다() {
        LocalDate startDate = LocalDate.of(2026, 6, 1);
        LocalDate endDate = LocalDate.of(2026, 6, 24);
        OrderService.SyncResult expected = new OrderService.SyncResult(10, 1, 0, null);
        given(orderService.backfillFromCafe24("mymall", startDate, endDate)).willReturn(expected);

        OrderService.SyncResult result = backfillService.backfillOrders("mymall", startDate, endDate);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void backfillProducts는_ProductService의_결과를_그대로_반환한다() {
        ProductService.SyncResult expected = new ProductService.SyncResult(5, 0, 1, "Cafe24 API 호출 실패", java.util.Set.of());
        given(productService.syncFromCafe24("mymall")).willReturn(expected);

        ProductService.SyncResult result = backfillService.backfillProducts("mymall");

        assertThat(result).isSameAs(expected);
    }

    @Test
    void backfillCarriers는_CarrierService의_결과를_그대로_반환한다() {
        CarrierService.SyncResult expected = new CarrierService.SyncResult(3, 0, 0, null);
        given(carrierService.syncFromCafe24("mymall")).willReturn(expected);

        CarrierService.SyncResult result = backfillService.backfillCarriers("mymall");

        assertThat(result).isSameAs(expected);
    }
}
