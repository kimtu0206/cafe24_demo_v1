package org.example.cafe24_demo_v1.product.domain.model;

import java.util.List;

/**
 * mallId 기준으로 페이지네이션된 상품 목록.
 *
 * Spring Data의 Page<T>를 도메인 레이어에 직접 노출하지 않기 위한 순수 결과 객체이다.
 */
public record ProductPage(List<Product> products, long totalCount) {
}
