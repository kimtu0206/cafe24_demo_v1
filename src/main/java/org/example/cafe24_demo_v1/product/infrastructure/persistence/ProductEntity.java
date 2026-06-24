package org.example.cafe24_demo_v1.product.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DB 테이블(cafe24_product)과 매핑되는 JPA 엔티티.
 * 도메인 모델(Product)과 분리된 별도 클래스이며, 외부에서 직접 사용하지 않도록
 * package-private으로 선언한다.
 */
@Entity
@Table(
        name = "cafe24_product",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mall_id", "product_no"})
)
@Getter
@Setter
class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_no", nullable = false)
    private Long productNo;   // Cafe24 상품 번호

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    private BigDecimal price;

    @Column(name = "supply_price")
    private BigDecimal supplyPrice;

    private String status; // ON_SALE / SUSPENDED

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "payment_info", columnDefinition = "TEXT")
    private String paymentInfo;

    @Column(name = "shipping_info", columnDefinition = "TEXT")
    private String shippingInfo;

    @Column(name = "exchange_info", columnDefinition = "TEXT")
    private String exchangeInfo;

    @Column(name = "price_excluding_tax")
    private BigDecimal priceExcludingTax;

    @Column(name = "detail_image")
    private String detailImage;

    @Column(name = "image_upload_type")
    private String imageUploadType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "missing_since")
    private LocalDateTime missingSince;

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
