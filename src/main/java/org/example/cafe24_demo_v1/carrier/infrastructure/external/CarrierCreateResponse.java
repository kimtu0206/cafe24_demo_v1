package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Cafe24 배송사 등록 API 응답 DTO. 실제 데이터는 "carrier" 키로 감싸져 온다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CarrierCreateResponse {
    private Cafe24CarrierPayload carrier;
}
