package org.example.cafe24_demo_v1.benefit.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Benefit {

    private Long id;
    private String mallId;
    private Integer shopNo;
    private Integer benefitNo;
    private String useBenefit;
    private String benefitName;
    private String benefitDivision;
    private String benefitType;
    private String useBenefitPeriod;
    private LocalDateTime benefitStartDate;
    private LocalDateTime benefitEndDate;
    private List<String> platformTypes;
    private String useGroupBinding;
    private List<Integer> customerGroupList;
    private String productBindingType;
    private String useExceptCategory;
    private String iconUrl;
    private String availableCoupon;
    private String repurchaseSale;
    private String bulkPurchaseSale;
    private String memberSale;
    private LocalDateTime createdDate;
    private PeriodSale periodSale;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Benefit() {}

    /** Cafe24 API 응답으로 새 혜택을 생성할 때 사용한다. */
    public static Benefit register(
            String mallId, Integer shopNo, Integer benefitNo, String useBenefit, String benefitName,
            String benefitDivision, String benefitType, String useBenefitPeriod,
            LocalDateTime benefitStartDate, LocalDateTime benefitEndDate,
            List<String> platformTypes, String useGroupBinding, List<Integer> customerGroupList,
            String productBindingType, String useExceptCategory, String iconUrl, String availableCoupon,
            String repurchaseSale, String bulkPurchaseSale, String memberSale,
            LocalDateTime createdDate, PeriodSale periodSale
    ) {
        Benefit b = new Benefit();
        b.mallId = mallId;
        b.shopNo = shopNo;
        b.benefitNo = benefitNo;
        b.useBenefit = useBenefit;
        b.benefitName = benefitName;
        b.benefitDivision = benefitDivision;
        b.benefitType = benefitType;
        b.useBenefitPeriod = useBenefitPeriod;
        b.benefitStartDate = benefitStartDate;
        b.benefitEndDate = benefitEndDate;
        b.platformTypes = platformTypes;
        b.useGroupBinding = useGroupBinding;
        b.customerGroupList = customerGroupList;
        b.productBindingType = productBindingType;
        b.useExceptCategory = useExceptCategory;
        b.iconUrl = iconUrl;
        b.availableCoupon = availableCoupon;
        b.repurchaseSale = repurchaseSale;
        b.bulkPurchaseSale = bulkPurchaseSale;
        b.memberSale = memberSale;
        b.createdDate = createdDate;
        b.periodSale = periodSale;
        b.createdAt = LocalDateTime.now();
        b.updatedAt = LocalDateTime.now();
        return b;
    }

    /** DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용한다. */
    public static Benefit reconstitute(
            Long id, String mallId, Integer shopNo, Integer benefitNo, String useBenefit, String benefitName,
            String benefitDivision, String benefitType, String useBenefitPeriod,
            LocalDateTime benefitStartDate, LocalDateTime benefitEndDate,
            List<String> platformTypes, String useGroupBinding, List<Integer> customerGroupList,
            String productBindingType, String useExceptCategory, String iconUrl, String availableCoupon,
            String repurchaseSale, String bulkPurchaseSale, String memberSale,
            LocalDateTime createdDate, PeriodSale periodSale,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        Benefit b = new Benefit();
        b.id = id;
        b.mallId = mallId;
        b.shopNo = shopNo;
        b.benefitNo = benefitNo;
        b.useBenefit = useBenefit;
        b.benefitName = benefitName;
        b.benefitDivision = benefitDivision;
        b.benefitType = benefitType;
        b.useBenefitPeriod = useBenefitPeriod;
        b.benefitStartDate = benefitStartDate;
        b.benefitEndDate = benefitEndDate;
        b.platformTypes = platformTypes;
        b.useGroupBinding = useGroupBinding;
        b.customerGroupList = customerGroupList;
        b.productBindingType = productBindingType;
        b.useExceptCategory = useExceptCategory;
        b.iconUrl = iconUrl;
        b.availableCoupon = availableCoupon;
        b.repurchaseSale = repurchaseSale;
        b.bulkPurchaseSale = bulkPurchaseSale;
        b.memberSale = memberSale;
        b.createdDate = createdDate;
        b.periodSale = periodSale;
        b.createdAt = createdAt;
        b.updatedAt = updatedAt;
        return b;
    }

    public void setId(Long id) { this.id = id; }
}
