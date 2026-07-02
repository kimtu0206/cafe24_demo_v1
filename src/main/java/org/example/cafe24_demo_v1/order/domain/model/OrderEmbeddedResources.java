package org.example.cafe24_demo_v1.order.domain.model;

import lombok.Getter;

/**
 * Cafe24 주문 조회 시 embed 파라미터로 함께 받아온 하위 리소스(품목/수령자/주문자/혜택/쿠폰/반품/취소/교환/환불)
 * 원본(JSON)을 묶은 불변 Value Object.
 *
 * 각 리소스는 별도 엔드포인트로 조회했을 때와 동일한 형태로 응답에 포함되지만, 정확한 필드 구조가 아직
 * 확인되지 않아 컬럼화하지 않고 원본 그대로 보존한다. Order.register/reconstitute/applySnapshot의
 * 파라미터가 계속 늘어나는 것을 막기 위해 도입했다(ProductRegistration과 같은 패턴).
 *
 * return은 MySQL 예약어이자 Java 키워드라 returnInfo로 대체한다.
 * benefits는 주문에 실제 적용된 혜택 이력이며, 쇼핑몰 혜택 마스터 데이터를 다루는 benefit 컨텍스트와는 다른 개념이다.
 */
@Getter
public class OrderEmbeddedResources {

    private final String items;
    private final String receivers;
    private final String buyer;
    private final String returnInfo;
    private final String cancellation;
    private final String exchange;
    private final String benefits;
    private final String coupons;
    private final String refunds;

    public OrderEmbeddedResources(
            String items,
            String receivers,
            String buyer,
            String returnInfo,
            String cancellation,
            String exchange,
            String benefits,
            String coupons,
            String refunds
    ) {
        this.items = items;
        this.receivers = receivers;
        this.buyer = buyer;
        this.returnInfo = returnInfo;
        this.cancellation = cancellation;
        this.exchange = exchange;
        this.benefits = benefits;
        this.coupons = coupons;
        this.refunds = refunds;
    }

    /** embed 응답이 없거나 하위 리소스가 비어 있을 때 사용한다. */
    public static OrderEmbeddedResources empty() {
        return new OrderEmbeddedResources(null, null, null, null, null, null, null, null, null);
    }
}
