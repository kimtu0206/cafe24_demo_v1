package org.example.cafe24_demo_v1.product.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Cafe24 상품 등록/단건 조회 API 응답 DTO. 실제 데이터는 "product" 키로 감싸져 온다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ProductCreateResponse {
    private Cafe24ProductPayload product;
}
