package org.example.cafe24_demo_v1.carrier.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DB 테이블(cafe24_carrier)과 매핑되는 JPA 엔티티.
 * 도메인 모델(Carrier)과 분리된 별도 클래스이며, 외부에서 직접 사용하지 않도록
 * package-private으로 선언한다.
 */
@Entity
@Table(
        name = "cafe24_carrier",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mall_id", "shipping_carrier_code"})
)
@Getter
@Setter
class CarrierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "carrier_id")
    private Long carrierId; // Cafe24 배송사 번호

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "shipping_carrier_code", nullable = false)
    private String shippingCarrierCode;

    @Column(name = "shipping_carrier_name", nullable = false)
    private String shippingCarrierName;

    private String contact;

    @Column(name = "secondary_contact")
    private String secondaryContact;

    private String email;

    @Column(name = "track_shipment_url")
    private String trackShipmentUrl;

    @Column(name = "default_shipping_fee")
    private BigDecimal defaultShippingFee;

    @Column(name = "homepage_url")
    private String homepageUrl;

    @Column(name = "shipping_type", nullable = false)
    private String shippingType;

    @Column(name = "default_carrier")
    private boolean defaultCarrier;

    @Column(name = "shipping_fee_setting")
    private boolean shippingFeeSetting;

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
