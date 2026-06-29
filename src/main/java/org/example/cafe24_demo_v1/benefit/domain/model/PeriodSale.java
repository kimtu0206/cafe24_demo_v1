package org.example.cafe24_demo_v1.benefit.domain.model;

import java.util.List;

public record PeriodSale(
        List<Integer> productList,
        List<Integer> addCategoryList,
        List<Integer> exceptCategoryList,
        Integer discountPurchasingQuantity,
        String discountValue,
        String discountValueUnit,
        String discountTruncationUnit,
        String discountTruncationMethod
) {}
