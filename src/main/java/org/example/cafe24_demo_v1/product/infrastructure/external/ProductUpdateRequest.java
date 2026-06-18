package org.example.cafe24_demo_v1.product.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Cafe24 상품 수정 API 요청 본문 DTO.
 * 등록 API와 같은 "shop_no" + "request" 형태를 쓰지만, display/selling/product_condition은
 * 보내지 않는다 — 이름/가격만 바꾸려는 의도인데 진열/판매 상태가 의도치 않게 초기화되는 것을 막기 위함.
 */
@Getter
class ProductUpdateRequest {

    @JsonProperty("shop_no")
    private final int shopNo = 1;

    private final Body request;

    ProductUpdateRequest(String productName, BigDecimal price, BigDecimal supplyPrice) {
        this.request = new Body(productName, price, supplyPrice);
    }

    @Getter
    static class Body {
        @JsonProperty("product_name")
        private final String productName;
        private final BigDecimal price;
        @JsonProperty("supply_price")
        private final BigDecimal supplyPrice;

        Body(String productName, BigDecimal price, BigDecimal supplyPrice) {
            this.productName = productName;
            this.price = price;
            this.supplyPrice = supplyPrice;
        }
    }
}
