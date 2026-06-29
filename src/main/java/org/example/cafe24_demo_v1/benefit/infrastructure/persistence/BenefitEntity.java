package org.example.cafe24_demo_v1.benefit.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cafe24_benefit",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mall_id", "benefit_no"})
)
@Getter
@Setter
class BenefitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mall_id", nullable = false)
    private String mallId;

    @Column(name = "shop_no")
    private Integer shopNo;

    @Column(name = "benefit_no", nullable = false)
    private Integer benefitNo;

    @Column(name = "use_benefit")
    private String useBenefit;

    @Column(name = "benefit_name")
    private String benefitName;

    @Column(name = "benefit_division")
    private String benefitDivision;

    @Column(name = "benefit_type")
    private String benefitType;

    @Column(name = "use_benefit_period")
    private String useBenefitPeriod;

    @Column(name = "benefit_start_date")
    private LocalDateTime benefitStartDate;

    @Column(name = "benefit_end_date")
    private LocalDateTime benefitEndDate;

    @Column(name = "platform_types")
    private String platformTypes;

    @Column(name = "use_group_binding")
    private String useGroupBinding;

    @Column(name = "customer_group_list")
    private String customerGroupList;

    @Column(name = "product_binding_type")
    private String productBindingType;

    @Column(name = "use_except_category")
    private String useExceptCategory;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    @Column(name = "available_coupon")
    private String availableCoupon;

    @Column(name = "repurchase_sale")
    private String repurchaseSale;

    @Column(name = "bulk_purchase_sale")
    private String bulkPurchaseSale;

    @Column(name = "member_sale")
    private String memberSale;

    @Column(name = "cafe24_created_date")
    private LocalDateTime cafe24CreatedDate;

    @Column(name = "period_sale", columnDefinition = "TEXT")
    private String periodSale;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
