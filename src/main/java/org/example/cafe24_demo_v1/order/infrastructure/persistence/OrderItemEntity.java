package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cafe24_order_item")
@Getter
@Setter
class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_fk_id", nullable = false)
    private Long orderFkId;

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "cafe24_order_id", nullable = false)
    private String cafe24OrderId;

    @Column(name = "shop_no")
    private Integer shopNo;

    @Column(name = "item_no")
    private Long itemNo;

    @Column(name = "order_item_code")
    private String orderItemCode;

    @Column(name = "product_no")
    private Long productNo;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "variant_code")
    private String variantCode;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "option_id")
    private String optionId;

    @Column(name = "option_value")
    private String optionValue;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "product_price", precision = 20, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "payment_amount", precision = 20, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "status_text")
    private String statusText;

    @Column(name = "tracking_no")
    private String trackingNo;

    @Column(name = "shipping_code")
    private String shippingCode;

    @Column(name = "shipping_company_name")
    private String shippingCompanyName;

    @Column(name = "ordered_date")
    private LocalDateTime orderedDate;

    @Column(name = "shipped_date")
    private LocalDateTime shippedDate;

    @Column(name = "delivered_date")
    private LocalDateTime deliveredDate;

    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;

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
