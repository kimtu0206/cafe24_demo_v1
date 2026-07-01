package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DB 테이블(cafe24_order)과 매핑되는 JPA 엔티티.
 * 도메인 모델(Order)과 분리된 별도 클래스이며, 외부에서 직접 사용하지 않도록
 * package-private으로 선언한다.
 */
@Entity
@Table(
        name = "cafe24_order",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mall_id", "order_id"})
)
@Getter
@Setter
class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private String orderId;   // Cafe24 주문번호 (예: 20200717-0029236)

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "order_type", length = 10)
    private String orderType;   // MEMBER(회원 주문) / GUEST(비회원 주문)

    @Column(name = "member_id")
    private String memberId;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_email")
    private String buyerEmail;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "ordered_at")
    private LocalDateTime orderedAt;

    @Column(name = "order_raw_json", columnDefinition = "TEXT")
    private String rawJson;   // Cafe24 응답 원본(JSON) — 컬럼화하지 않은 나머지 정보 보존용

    @Column(name = "items", columnDefinition = "TEXT")
    private String items;   // embed=items 응답 원본(JSON)

    @Column(name = "receivers", columnDefinition = "TEXT")
    private String receivers;   // embed=receivers 응답 원본(JSON)

    @Column(name = "buyer", columnDefinition = "TEXT")
    private String buyer;   // embed=buyer 응답 원본(JSON)

    // return은 MySQL 예약어라 컬럼명을 return_info로 둔다.
    @Column(name = "return_info", columnDefinition = "TEXT")
    private String returnInfo;   // embed=return 응답 원본(JSON)

    @Column(name = "cancellation", columnDefinition = "TEXT")
    private String cancellation;   // embed=cancellation 응답 원본(JSON)

    @Column(name = "exchange", columnDefinition = "TEXT")
    private String exchange;   // embed=exchange 응답 원본(JSON)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}