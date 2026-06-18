package org.example.cafe24_demo_v1.product.domain.model;

/**
 * Cafe24 상품의 진열(display)/판매(selling) 플래그를 가공한 도메인 상태.
 * 두 플래그가 모두 "T"(진열중 + 판매중)일 때만 ON_SALE으로 판단한다.
 */
public enum ProductStatus {
    ON_SALE,
    SUSPENDED;

    public static ProductStatus from(String display, String selling) {
        boolean displayed = "T".equalsIgnoreCase(display);
        boolean isSelling = "T".equalsIgnoreCase(selling);
        return (displayed && isSelling) ? ON_SALE : SUSPENDED;
    }
}
