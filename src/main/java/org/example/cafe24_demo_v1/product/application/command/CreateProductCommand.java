package org.example.cafe24_demo_v1.product.application.command;

import java.math.BigDecimal;

public record CreateProductCommand(String mallId, String productName, BigDecimal price, BigDecimal supplyPrice) {
}
