package org.example.cafe24_demo_v1.product.application.command;

import java.math.BigDecimal;

public record CreateProductCommand(
        String mallId,
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
}
