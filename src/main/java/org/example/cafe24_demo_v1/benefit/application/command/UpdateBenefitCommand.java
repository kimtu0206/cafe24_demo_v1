package org.example.cafe24_demo_v1.benefit.application.command;

import java.util.List;

public record UpdateBenefitCommand(
        String mallId,
        Integer benefitNo,
        Integer shopNo,
        String useBenefit,
        String benefitName,
        String useBenefitPeriod,
        String benefitStartDate,
        String benefitEndDate,
        List<String> platformTypes,
        String useGroupBinding,
        List<Integer> customerGroupList,
        String productBindingType,
        String useExceptCategory,
        String availableCoupon,
        String iconUrl,
        PeriodSaleCommand periodSale
) {
    public record PeriodSaleCommand(
            List<Integer> productList,
            List<Integer> exceptCategoryList,
            String discountValue,
            String discountValueUnit,
            String discountTruncationUnit,
            String discountTruncationMethod
    ) {}
}
