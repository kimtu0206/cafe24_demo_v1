package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cafe24_demo_v1.order.domain.model.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderItemMapperTest {

    private final OrderItemMapper mapper = new OrderItemMapper(new ObjectMapper());

    private static final String SAMPLE_ITEMS_JSON = """
            [
              {
                "shop_no": 1,
                "item_no": 47,
                "order_item_code": "20260622-0000054-01",
                "variant_code": "P00000EY000A",
                "product_no": 128,
                "product_code": "P00000EY",
                "product_name": "iPhone se",
                "option_id": "000A",
                "option_value": "",
                "product_price": "11000.00",
                "payment_amount": "11000.00",
                "quantity": 1,
                "order_status": "N30",
                "status_code": "N1",
                "status_text": "배송중",
                "tracking_no": "123412341234",
                "shipping_code": "D-20260622-0000054-00",
                "shipping_company_name": "우체국택배",
                "ordered_date": "2026-06-23T14:32:23+09:00",
                "shipped_date": "2026-06-23T14:33:52+09:00",
                "delivered_date": null
              }
            ]
            """;

    @Test
    void fromItemsJson_모든_필드를_정상_파싱한다() {
        List<OrderItem> items = mapper.fromItemsJson(10L, "testmall", "20260622-0000054", SAMPLE_ITEMS_JSON);

        assertThat(items).hasSize(1);
        OrderItem item = items.get(0);

        assertThat(item.getOrderFkId()).isEqualTo(10L);
        assertThat(item.getMallId()).isEqualTo("testmall");
        assertThat(item.getCafe24OrderId()).isEqualTo("20260622-0000054");
        assertThat(item.getShopNo()).isEqualTo(1);
        assertThat(item.getItemNo()).isEqualTo(47L);
        assertThat(item.getOrderItemCode()).isEqualTo("20260622-0000054-01");
        assertThat(item.getVariantCode()).isEqualTo("P00000EY000A");
        assertThat(item.getProductNo()).isEqualTo(128L);
        assertThat(item.getProductCode()).isEqualTo("P00000EY");
        assertThat(item.getProductName()).isEqualTo("iPhone se");
        assertThat(item.getOptionId()).isEqualTo("000A");
        assertThat(item.getOptionValue()).isEmpty();
        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(item.getProductPrice()).isEqualByComparingTo(new BigDecimal("11000.00"));
        assertThat(item.getPaymentAmount()).isEqualByComparingTo(new BigDecimal("11000.00"));
        assertThat(item.getOrderStatus()).isEqualTo("N30");
        assertThat(item.getStatusCode()).isEqualTo("N1");
        assertThat(item.getStatusText()).isEqualTo("배송중");
        assertThat(item.getTrackingNo()).isEqualTo("123412341234");
        assertThat(item.getShippingCode()).isEqualTo("D-20260622-0000054-00");
        assertThat(item.getShippingCompanyName()).isEqualTo("우체국택배");
        assertThat(item.getOrderedDate()).isEqualTo(LocalDateTime.of(2026, 6, 23, 14, 32, 23));
        assertThat(item.getShippedDate()).isEqualTo(LocalDateTime.of(2026, 6, 23, 14, 33, 52));
        assertThat(item.getDeliveredDate()).isNull();
        assertThat(item.getRawJson()).isNotNull();
    }

    @Test
    void fromItemsJson_items가_null이면_빈_리스트를_반환한다() {
        List<OrderItem> items = mapper.fromItemsJson(1L, "mall", "order-1", null);
        assertThat(items).isEmpty();
    }

    @Test
    void fromItemsJson_items가_빈_문자열이면_빈_리스트를_반환한다() {
        List<OrderItem> items = mapper.fromItemsJson(1L, "mall", "order-1", "");
        assertThat(items).isEmpty();
    }

    @Test
    void fromItemsJson_빈_배열이면_빈_리스트를_반환한다() {
        List<OrderItem> items = mapper.fromItemsJson(1L, "mall", "order-1", "[]");
        assertThat(items).isEmpty();
    }

    @Test
    void fromItemsJson_날짜_필드가_null이어도_나머지_필드는_정상_파싱한다() {
        String json = """
                [{"shop_no": 1, "item_no": 1, "product_name": "테스트상품",
                  "ordered_date": null, "shipped_date": null, "delivered_date": null}]
                """;

        List<OrderItem> items = mapper.fromItemsJson(1L, "mall", "order-1", json);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductName()).isEqualTo("테스트상품");
        assertThat(items.get(0).getOrderedDate()).isNull();
    }

    @Test
    void fromItemsJson_여러_품목이면_모두_파싱한다() {
        String json = """
                [
                  {"shop_no": 1, "item_no": 1, "order_item_code": "CODE-01"},
                  {"shop_no": 1, "item_no": 2, "order_item_code": "CODE-02"}
                ]
                """;

        List<OrderItem> items = mapper.fromItemsJson(5L, "mall", "order-multi", json);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getOrderItemCode()).isEqualTo("CODE-01");
        assertThat(items.get(1).getOrderItemCode()).isEqualTo("CODE-02");
        assertThat(items).allMatch(item -> item.getOrderFkId().equals(5L));
    }
}
