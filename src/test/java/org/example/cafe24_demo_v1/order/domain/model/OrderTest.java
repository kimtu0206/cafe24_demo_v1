package org.example.cafe24_demo_v1.order.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void register로_생성하면_입력한_값으로_초기화된다() {
        LocalDateTime orderedAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        OrderEmbeddedResources embeds = new OrderEmbeddedResources(
                "[{\"item_no\":1}]", "[{\"receiver_name\":\"홍길동\"}]", "{\"name\":\"홍길동\"}", null, null, null
        );
        Order order = Order.register(
                "mymall", "20200717-0029236", "N40", "gdhong", "Jessica Hong", "gdhong@cafe24corp.com",
                new BigDecimal("24680.00"), "mileage", orderedAt, "{\"order_id\":\"20200717-0029236\"}", null, null, embeds
        );

        assertThat(order.getMallId()).isEqualTo("mymall");
        assertThat(order.getOrderId()).isEqualTo("20200717-0029236");
        assertThat(order.getOrderStatus()).isEqualTo("N40");
        assertThat(order.getMemberId()).isEqualTo("gdhong");
        assertThat(order.getBuyerName()).isEqualTo("Jessica Hong");
        assertThat(order.getBuyerEmail()).isEqualTo("gdhong@cafe24corp.com");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("24680.00"));
        assertThat(order.getPaymentMethod()).isEqualTo("mileage");
        assertThat(order.getOrderType()).isEqualTo(OrderType.MEMBER);
        assertThat(order.getOrderedAt()).isEqualTo(orderedAt);
        assertThat(order.getRawJson()).contains("20200717-0029236");
        assertThat(order.getItems()).isEqualTo("[{\"item_no\":1}]");
        assertThat(order.getReceivers()).isEqualTo("[{\"receiver_name\":\"홍길동\"}]");
        assertThat(order.getBuyer()).isEqualTo("{\"name\":\"홍길동\"}");
        assertThat(order.getReturnInfo()).isNull();
        assertThat(order.getCancellation()).isNull();
        assertThat(order.getExchange()).isNull();
        assertThat(order.getCreatedAt()).isNotNull();
    }

    @Test
    void applySnapshot으로_주문_정보를_갱신할_수_있다() {
        Order order = Order.register(
                "mymall", "20200717-0029236", "N10", "gdhong", "기존 이름", "old@cafe24corp.com",
                new BigDecimal("1000"), "card", LocalDateTime.of(2024, 1, 1, 0, 0), "{}", null, null, OrderEmbeddedResources.empty()
        );

        order.applySnapshot(
                "N40", "gdhong", "변경된 이름", "new@cafe24corp.com",
                new BigDecimal("2000"), "mileage", LocalDateTime.of(2024, 1, 2, 0, 0), "{\"changed\":true}",
                null, null,
                new OrderEmbeddedResources(null, null, null, "{\"return_no\":1}", null, null)
        );

        assertThat(order.getOrderStatus()).isEqualTo("N40");
        assertThat(order.getBuyerName()).isEqualTo("변경된 이름");
        assertThat(order.getBuyerEmail()).isEqualTo("new@cafe24corp.com");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("2000"));
        assertThat(order.getPaymentMethod()).isEqualTo("mileage");
        assertThat(order.getOrderedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 0, 0));
        assertThat(order.getRawJson()).isEqualTo("{\"changed\":true}");
        assertThat(order.getReturnInfo()).isEqualTo("{\"return_no\":1}");
        assertThat(order.getOrderType()).isEqualTo(OrderType.MEMBER);
    }

    @Test
    void memberId가_null이면_GUEST로_분류된다() {
        Order order = Order.register(
                "mymall", "20200717-9999999", "N10", null, null, null,
                null, null, LocalDateTime.now(), "{}", null, null, OrderEmbeddedResources.empty()
        );

        assertThat(order.getOrderType()).isEqualTo(OrderType.GUEST);
        assertThat(order.getMemberId()).isNull();
    }

    @Test
    void memberId가_빈문자열이면_GUEST로_분류된다() {
        Order order = Order.register(
                "mymall", "20200717-9999999", "N10", "", null, null,
                null, null, LocalDateTime.now(), "{}", null, null, OrderEmbeddedResources.empty()
        );

        assertThat(order.getOrderType()).isEqualTo(OrderType.GUEST);
    }

    @Test
    void applySnapshot으로_비회원_갱신_시_GUEST로_재분류된다() {
        Order order = Order.register(
                "mymall", "20200717-0029236", "N10", "gdhong", "기존 이름", "old@cafe24corp.com",
                new BigDecimal("1000"), "card", LocalDateTime.now(), "{}", null, null, OrderEmbeddedResources.empty()
        );

        order.applySnapshot(
                "N40", null, null, null,
                new BigDecimal("1000"), "card", LocalDateTime.now(), "{}",
                null, null,
                OrderEmbeddedResources.empty()
        );

        assertThat(order.getOrderType()).isEqualTo(OrderType.GUEST);
    }

    @Test
    void getEmbeds는_현재_보유한_하위_리소스를_VO로_묶어서_반환한다() {
        OrderEmbeddedResources embeds = new OrderEmbeddedResources(
                "items-json", "receivers-json", "buyer-json", "return-json", "cancellation-json", "exchange-json"
        );
        Order order = Order.register(
                "mymall", "20200717-0029236", "N40", "gdhong", "Jessica Hong", "gdhong@cafe24corp.com",
                new BigDecimal("1000"), "card", LocalDateTime.now(), "{}", null, null, embeds
        );

        OrderEmbeddedResources result = order.getEmbeds();

        assertThat(result.getItems()).isEqualTo("items-json");
        assertThat(result.getReceivers()).isEqualTo("receivers-json");
        assertThat(result.getBuyer()).isEqualTo("buyer-json");
        assertThat(result.getReturnInfo()).isEqualTo("return-json");
        assertThat(result.getCancellation()).isEqualTo("cancellation-json");
        assertThat(result.getExchange()).isEqualTo("exchange-json");
    }
}
