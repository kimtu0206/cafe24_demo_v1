package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * Cafe24 배송사 등록 API 요청 봉투(envelope).
 * Cafe24 Admin API의 쓰기 요청 공통 규칙대로 실제 필드는 "request" 키 아래에 감싸고,
 * 멀티샵 식별자 shop_no를 최상위에 함께 보낸다(이 데모는 단일 쇼핑몰이라 1로 고정).
 */
@Getter
class CarrierCreateRequest {

    @JsonProperty("shop_no")
    private final int shopNo = 1;

    private final CarrierCreateRequestPayload request;

    CarrierCreateRequest(CarrierCreateRequestPayload request) {
        this.request = request;
    }
}
