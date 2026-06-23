package org.example.cafe24_demo_v1.webhook.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cafe24 Webhook 요청 바디의 공통 구조.
 * 이벤트 종류와 무관하게 Cafe24는 항상 이 포맷으로 보낸다.
 */
record Cafe24WebhookPayload(
        @JsonProperty("event_no") Integer eventNo,
        Resource resource
) {

    /** 이벤트가 발생한 쇼핑몰과 리소스 정보 */
    record Resource(
            @JsonProperty("mall_id") String mallId,
            @JsonProperty("client_id") String clientId,
            @JsonProperty("app_name") String appName,
            @JsonProperty("deleted_date") String deletedDate, // 앱 삭제 이벤트의 경우 삭제 일시
            @JsonProperty("product_no") Long productNo,       // 상품 이벤트의 경우 상품 번호
            @JsonProperty("sc_id") String shippingCarrierCode // 배송사 이벤트의 경우 배송사 코드(Cafe24 웹훅은 REST API와 달리 sc_id로 보냄)
    ) {}
}
