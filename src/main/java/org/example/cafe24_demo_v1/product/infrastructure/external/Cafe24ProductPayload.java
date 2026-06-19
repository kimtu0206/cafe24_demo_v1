package org.example.cafe24_demo_v1.product.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Cafe24 Admin 상품 API가 반환하는 "product" 객체를 역직렬화하는 DTO.
 * infrastructure 레이어 내부에서만 사용하며, 도메인 레이어로 직접 노출되지 않는다.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24ProductPayload {

    @JsonProperty("product_no")
    private Long productNo;

    @JsonProperty("product_name")
    private String productName;

    private BigDecimal price;

    @JsonProperty("supply_price")
    private BigDecimal supplyPrice;

    private String display;

    private String selling;

    private String description;

    @JsonProperty("payment_info")
    private String paymentInfo;

    @JsonProperty("shipping_info")
    private String shippingInfo;

    @JsonProperty("exchange_info")
    private String exchangeInfo;

    @JsonProperty("price_excluding_tax")
    private BigDecimal priceExcludingTax;

    @JsonProperty("detail_image")
    private String detailImage;

    @JsonProperty("image_upload_type")
    private String imageUploadType;
}
