package org.example.cafe24_demo_v1.order.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class OrderItem {

    private Long id;
    private Long orderFkId;          // cafe24_order.id FK
    private String mallId;
    private String cafe24OrderId;    // Cafe24 주문번호
    private Integer shopNo;
    private Long itemNo;
    private String orderItemCode;
    private Long productNo;
    private String productCode;
    private String variantCode;
    private String productName;
    private String optionId;
    private String optionValue;
    private Integer quantity;
    private BigDecimal productPrice;
    private BigDecimal paymentAmount;
    private String orderStatus;
    private String statusCode;
    private String statusText;
    private String trackingNo;
    private String shippingCode;
    private String shippingCompanyName;
    private LocalDateTime orderedDate;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;
    private String rawJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private OrderItem() {}

    public static OrderItem of(
            Long orderFkId,
            String mallId,
            String cafe24OrderId,
            Integer shopNo,
            Long itemNo,
            String orderItemCode,
            Long productNo,
            String productCode,
            String variantCode,
            String productName,
            String optionId,
            String optionValue,
            Integer quantity,
            BigDecimal productPrice,
            BigDecimal paymentAmount,
            String orderStatus,
            String statusCode,
            String statusText,
            String trackingNo,
            String shippingCode,
            String shippingCompanyName,
            LocalDateTime orderedDate,
            LocalDateTime shippedDate,
            LocalDateTime deliveredDate,
            String rawJson
    ) {
        OrderItem item = new OrderItem();
        item.orderFkId = orderFkId;
        item.mallId = mallId;
        item.cafe24OrderId = cafe24OrderId;
        item.shopNo = shopNo;
        item.itemNo = itemNo;
        item.orderItemCode = orderItemCode;
        item.productNo = productNo;
        item.productCode = productCode;
        item.variantCode = variantCode;
        item.productName = productName;
        item.optionId = optionId;
        item.optionValue = optionValue;
        item.quantity = quantity;
        item.productPrice = productPrice;
        item.paymentAmount = paymentAmount;
        item.orderStatus = orderStatus;
        item.statusCode = statusCode;
        item.statusText = statusText;
        item.trackingNo = trackingNo;
        item.shippingCode = shippingCode;
        item.shippingCompanyName = shippingCompanyName;
        item.orderedDate = orderedDate;
        item.shippedDate = shippedDate;
        item.deliveredDate = deliveredDate;
        item.rawJson = rawJson;
        return item;
    }

    public void setId(Long id) { this.id = id; }
}
