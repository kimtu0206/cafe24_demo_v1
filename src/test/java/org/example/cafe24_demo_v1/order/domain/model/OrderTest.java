package org.example.cafe24_demo_v1.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void register로_생성하면_입력한_값으로_초기화된다() {
        LocalDateTime orderedAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        Order order = Order.register(
                "mymall", "20200717-0029236", "N40", "gdhong", "Jessica Hong", "gdhong@cafe24corp.com",
                new BigDecimal("24680.00"), "mileage", orderedAt, "{\"order_id\":\"20200717-0029236\"}"
        );

        assertThat(order.getMallId()).isEqualTo("mymall");
        assertThat(order.getOrderId()).isEqualTo("20200717-0029236");
        assertThat(order.getOrderStatus()).isEqualTo("N40");
        assertThat(order.getMemberId()).isEqualTo("gdhong");
        assertThat(order.getBuyerName()).isEqualTo("Jessica Hong");
        assertThat(order.getBuyerEmail()).isEqualTo("gdhong@cafe24corp.com");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("24680.00"));
        assertThat(order.getPaymentMethod()).isEqualTo("mileage");
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
        assertThat(order.getRawJson()).contains("20200717-0029236");
        assertThat(order.getCreatedAt()).isNotNull();
    }

    @Test
    void applySnapshot으로_주문_정보를_갱신할_수_있다() {
        Order order = Order.register(
                "mymall", "20200717-0029236", "N10", "gdhong", "기존 이름", "old@cafe24corp.com",
                new BigDecimal("1000"), "card", LocalDateTime.of(2024, 1, 1, 0, 0), "{}"
        );

        order.applySnapshot(
                "N40", "gdhong", "변경된 이름", "new@cafe24corp.com",
                new BigDecimal("2000"), "mileage", LocalDateTime.of(2024, 1, 2, 0, 0), "{\"changed\":true}"
        );

        assertThat(order.getOrderStatus()).isEqualTo("N40");
        assertThat(order.getBuyerName()).isEqualTo("변경된 이름");
        assertThat(order.getBuyerEmail()).isEqualTo("new@cafe24corp.com");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("2000"));
        assertThat(order.getPaymentMethod()).isEqualTo("mileage");
        assertThat(order.getOrderedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 0, 0));
        assertThat(order.getRawJson()).isEqualTo("{\"changed\":true}");
    }
}
