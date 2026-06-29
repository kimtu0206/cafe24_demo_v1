package org.example.cafe24_demo_v1.benefit.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24BenefitPayload {

    @JsonProperty("shop_no")
    private Integer shopNo;

    @JsonProperty("benefit_no")
    private Integer benefitNo;

    @JsonProperty("use_benefit")
    private String useBenefit;

    @JsonProperty("benefit_name")
    private String benefitName;

    @JsonProperty("benefit_division")
    private String benefitDivision;

    @JsonProperty("benefit_type")
    private String benefitType;

    @JsonProperty("use_benefit_period")
    private String useBenefitPeriod;

    @JsonProperty("benefit_start_date")
    private OffsetDateTime benefitStartDate;

    @JsonProperty("benefit_end_date")
    private OffsetDateTime benefitEndDate;

    @JsonProperty("platform_types")
    private List<String> platformTypes;

    @JsonProperty("use_group_binding")
    private String useGroupBinding;

    @JsonProperty("customer_group_list")
    private List<Integer> customerGroupList;

    @JsonProperty("product_binding_type")
    private String productBindingType;

    @JsonProperty("use_except_category")
    private String useExceptCategory;

    @JsonProperty("icon_url")
    private String iconUrl;

    @JsonProperty("available_coupon")
    private String availableCoupon;

    @JsonProperty("repurchase_sale")
    private String repurchaseSale;

    @JsonProperty("bulk_purchase_sale")
    private String bulkPurchaseSale;

    @JsonProperty("member_sale")
    private String memberSale;

    @JsonProperty("created_date")
    private OffsetDateTime createdDate;

    @JsonProperty("period_sale")
    private PeriodSalePayload periodSale;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PeriodSalePayload {
        @JsonProperty("product_list")
        private List<Integer> productList;

        @JsonProperty("add_category_list")
        private List<Integer> addCategoryList;

        @JsonProperty("except_category_list")
        private List<Integer> exceptCategoryList;

        @JsonProperty("discount_purchasing_quantity")
        private Integer discountPurchasingQuantity;

        @JsonProperty("discount_value")
        private String discountValue;

        @JsonProperty("discount_value_unit")
        private String discountValueUnit;

        @JsonProperty("discount_truncation_unit")
        private String discountTruncationUnit;

        @JsonProperty("discount_truncation_method")
        private String discountTruncationMethod;
    }
}
