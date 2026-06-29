package org.example.cafe24_demo_v1.webhook.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cafe24 주문 생성 Webhook 요청 바디.
 *
 * 주문 resource는 필드가 매우 많고(40개 이상) Cafe24 스펙 변경 가능성도 있어,
 * 상품/앱삭제 Webhook과 달리 고정 필드를 둔 record로 매핑하지 않는다. 대신 resource를
 * JsonNode로 받아 라우팅에 필요한 mall_id/order_id만 꺼내 쓰고, 전체는 원본 그대로 보존한다.
 */
record OrderWebhookPayload(
        @Schema(example = "1234") @JsonProperty("event_no") Integer eventNo,
        @Schema(type = "object", example = "{\"mall_id\": \"testmall\", \"order_id\": \"20240115-0000011\", \"payment_gateway_name\": \"creditcard\", \"currency\": \"KRW\", \"initial_order_amount\": {\"order_price_amount\": \"89000.00\"}}") JsonNode resource
) {

    String mallId() {
        return resource == null ? null : resource.path("mall_id").asText(null);
    }

    String orderId() {
        return resource == null ? null : resource.path("order_id").asText(null);
    }
}
