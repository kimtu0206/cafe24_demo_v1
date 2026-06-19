package org.example.cafe24_demo_v1.product.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.example.cafe24_demo_v1.product.domain.model.ProductRegistration;

import java.math.BigDecimal;

/**
 * Cafe24 상품 수정 API 요청 본문 DTO.
 * 등록 API와 같은 "shop_no" + "request" 형태를 쓰지만, display/selling/product_condition은
 * 보내지 않는다 — 이름/가격만 바꾸려는 의도인데 진열/판매 상태가 의도치 않게 초기화되는 것을 막기 위함.
 * 같은 이유로 description 등 선택 필드도 값이 없으면(null) JSON에서 제외해(@JsonInclude) Cafe24에
 * 저장된 기존 값이 의도치 않게 지워지지 않도록 한다.
 */
@Getter
class ProductUpdateRequest {

    @JsonProperty("shop_no")
    private final int shopNo = 1;

    private final Body request;

    ProductUpdateRequest(ProductRegistration registration) {
        this.request = new Body(registration);
    }

    @Getter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Body {
        @JsonProperty("product_name")
        private final String productName;
        private final BigDecimal price;
        @JsonProperty("supply_price")
        private final BigDecimal supplyPrice;
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
