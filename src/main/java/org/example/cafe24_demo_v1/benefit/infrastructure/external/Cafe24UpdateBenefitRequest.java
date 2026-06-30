package org.example.cafe24_demo_v1.benefit.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.example.cafe24_demo_v1.benefit.application.command.UpdateBenefitCommand;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
class Cafe24UpdateBenefitRequest {

    @JsonProperty("shop_no")
    private Integer shopNo;

    @JsonProperty("request")
    private BenefitRequestBody request;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class BenefitRequestBody {
        @JsonProperty("use_benefit")
        private String useBenefit;

        @JsonProperty("benefit_name")
        private String benefitName;

        @JsonProperty("use_benefit_period")
        private String useBenefitPeriod;

        @JsonProperty("benefit_start_date")
        private String benefitStartDate;

        @JsonProperty("benefit_end_date")
        private String benefitEndDate;

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

        @JsonProperty("available_coupon")
        private String availableCoupon;

        @JsonProperty("icon_url")
        private String iconUrl;

        @JsonProperty("period_sale")
        private PeriodSaleBody periodSale;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class PeriodSaleBody {
        @JsonProperty("product_list")
        private List<Integer> productList;

        @JsonProperty("except_category_list")
        private List<Integer> exceptCategoryList;

        @JsonProperty("discount_value")
        private String discountValue;

        @JsonProperty("discount_value_unit")
        private String discountValueUnit;

        @JsonProperty("discount_truncation_unit")
        private String discountTruncationUnit;

        @JsonProperty("discount_truncation_method")
        private String discountTruncationMethod;
    }

    static Cafe24UpdateBenefitRequest from(UpdateBenefitCommand command) {
        Cafe24UpdateBenefitRequest req = new Cafe24UpdateBenefitRequest();
        req.setShopNo(command.shopNo());

        BenefitRequestBody body = new BenefitRequestBody();
        body.setUseBenefit(command.useBenefit());
        body.setBenefitName(command.benefitName());
        body.setUseBenefitPeriod(command.useBenefitPeriod());
        body.setBenefitStartDate(command.benefitStartDate());
        body.setBenefitEndDate(command.benefitEndDate());
        body.setPlatformTypes(command.platformTypes());
        body.setUseGroupBinding(command.useGroupBinding());
        body.setCustomerGroupList(command.customerGroupList());
        body.setProductBindingType(command.productBindingType());
        body.setUseExceptCategory(command.useExceptCategory());
        body.setAvailableCoupon(command.availableCoupon());
        body.setIconUrl(command.iconUrl());

        if (command.periodSale() != null) {
            PeriodSaleBody ps = new PeriodSaleBody();
            ps.setProductList(command.periodSale().productList());
            ps.setExceptCategoryList(command.periodSale().exceptCategoryList());
            ps.setDiscountValue(command.periodSale().discountValue());
            ps.setDiscountValueUnit(command.periodSale().discountValueUnit());
            ps.setDiscountTruncationUnit(command.periodSale().discountTruncationUnit());
            ps.setDiscountTruncationMethod(command.periodSale().discountTruncationMethod());
            body.setPeriodSale(ps);
        }

        req.setRequest(body);
        return req;
    }
}
