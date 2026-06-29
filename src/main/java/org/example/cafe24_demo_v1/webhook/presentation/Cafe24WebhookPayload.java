package org.example.cafe24_demo_v1.webhook.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Cafe24 Webhook 요청 바디의 공통 구조.
 * 이벤트 종류와 무관하게 Cafe24는 항상 이 포맷으로 보낸다.
 */
record Cafe24WebhookPayload(
        @Schema(example = "1234") @JsonProperty("event_no") Integer eventNo,
        Resource resource
) {

    /** 이벤트가 발생한 쇼핑몰과 리소스 정보 */
    record Resource(
            @Schema(example = "testmall") @JsonProperty("mall_id") String mallId,
            @Schema(example = "abcdefghij1234567890") @JsonProperty("client_id") String clientId,
            @Schema(example = "TestApp") @JsonProperty("app_name") String appName,
            @Schema(example = "2024-01-15T10:30:00+09:00") @JsonProperty("deleted_date") String deletedDate, // 앱 삭제 이벤트의 경우 삭제 일시
            @Schema(example = "12345678") @JsonProperty("product_no") Long productNo,       // 상품 이벤트의 경우 상품 번호
            @Schema(example = "0001") @JsonProperty("sc_id") String shippingCarrierCode, // 배송사 이벤트의 경우 배송사 코드(Cafe24 웹훅은 REST API와 달리 sc_id로 보냄)
            @Schema(example = "1000") @JsonProperty("benefit_no") Integer benefitNo      // 혜택 이벤트의 경우 혜택 번호
    ) {}
}
