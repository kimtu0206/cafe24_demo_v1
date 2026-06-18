package org.example.cafe24_demo_v1.product.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Cafe24 상품 목록 조회 API 응답 DTO. 실제 데이터는 "products" 키로 감싸져 온다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class ProductListResponse {
    private List<Cafe24ProductPayload> products;
}
