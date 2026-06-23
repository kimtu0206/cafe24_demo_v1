package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Cafe24 배송사 목록 조회 API 응답 DTO. 실제 데이터는 "carriers" 키로 감싸져 온다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class CarrierListResponse {
    private List<Cafe24CarrierPayload> carriers;
}
