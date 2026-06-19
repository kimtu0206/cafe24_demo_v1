package org.example.cafe24_demo_v1.product.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Cafe24에 신규 상품을 등록할 때 필요한 입력값을 묶은 불변(Immutable) Value Object.
 *
 * Cafe24ProductPort.createProduct()의 파라미터가 계속 늘어나는 것을 막기 위해 도입했다.
 */
@Getter
public class ProductRegistration {

    private final String productName;
    private final BigDecimal price;
    private final BigDecimal supplyPrice;
    private final String description;
    private final String paymentInfo;
    private final String shippingInfo;
    private final String exchangeInfo;
    private final BigDecimal priceExcludingTax;
    private final String detailImage;
    private final String imageUploadType;

    public ProductRegistration(
            String productName,
            BigDecimal price,
            BigDecimal supplyPrice,
            String description,
            String paymentInfo,
            String shippingInfo,
            String exchangeInfo,
            BigDecimal priceExcludingTax,
            String detailImage,
            String imageUploadType
    ) {
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(price, "price must not be null");
        Objects.requireNonNull(supplyPrice, "supplyPrice must not be null");
        this.productName = productName;
        this.price = price;
        this.supplyPrice = supplyPrice;
        this.description = description;
        this.paymentInfo = paymentInfo;
        this.shippingInfo = shippingInfo;
        this.exchangeInfo = exchangeInfo;
        this.priceExcludingTax = priceExcludingTax;
        this.detailImage = detailImage;
        this.imageUploadType = imageUploadType;
    }
}
