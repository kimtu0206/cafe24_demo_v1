package org.example.cafe24_demo_v1.product.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.example.cafe24_demo_v1.product.domain.model.ProductRegistration;

import java.math.BigDecimal;

/**
 * Cafe24 상품 등록 API 요청 본문 DTO.
 * Cafe24 Admin API는 "shop_no"와 실제 데이터를 담은 "request"를 같은 레벨(형제 필드)로 요구한다.
 */
@Getter
class ProductCreateRequest {

    @JsonProperty("shop_no")
    private final int shopNo = 1; // 멀티쇼핑몰을 사용하지 않는 데모 범위이므로 기본 쇼핑몰(1)로 고정

    private final Body request;

    ProductCreateRequest(ProductRegistration registration) {
        this.request = new Body(registration);
    }

    @Getter
    static class Body {
        @JsonProperty("product_name")
        private final String productName;
        private final BigDecimal price;
        @JsonProperty("supply_price")
        private final BigDecimal supplyPrice;
        private final String display = "T";
        private final String selling = "T";
        @JsonProperty("product_condition")
        private final String productCondition = "N";
        private final String description;
        @JsonProperty("payment_info")
        private final String paymentInfo;
        @JsonProperty("shipping_info")
        private final String shippingInfo;
        @JsonProperty("exchange_info")
        private final String exchangeInfo;
        @JsonProperty("price_excluding_tax")
        private final BigDecimal priceExcludingTax;
        @JsonProperty("detail_image")
        private final String detailImage;
        @JsonProperty("image_upload_type")
        private final String imageUploadType;

        Body(ProductRegistration registration) {
            this.productName = registration.getProductName();
            this.price = registration.getPrice();
            this.supplyPrice = registration.getSupplyPrice();
            this.description = registration.getDescription();
            this.paymentInfo = registration.getPaymentInfo();
            this.shippingInfo = registration.getShippingInfo();
            this.exchangeInfo = registration.getExchangeInfo();
            this.priceExcludingTax = registration.getPriceExcludingTax();
            this.detailImage = registration.getDetailImage();
            this.imageUploadType = registration.getImageUploadType();
        }
    }
}
