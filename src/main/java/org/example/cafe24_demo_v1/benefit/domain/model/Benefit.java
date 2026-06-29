package org.example.cafe24_demo_v1.benefit.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Benefit {
    private final Integer shopNo;
    private final Integer benefitNo;
    private final String useBenefit;
    private final String benefitName;
    private final String benefitDivision;
    private final String benefitType;
    private final String useBenefitPeriod;
    private final LocalDateTime benefitStartDate;
    private final LocalDateTime benefitEndDate;
    private final List<String> platformTypes;
    private final String useGroupBinding;
    private final List<Integer> customerGroupList;
    private final String productBindingType;
    private final String useExceptCategory;
    private final String iconUrl;
    private final String availableCoupon;
    private final String repurchaseSale;
    private final String bulkPurchaseSale;
    private final String memberSale;
}
