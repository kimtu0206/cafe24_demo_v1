package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
class OrderItemMapper {

    private final ObjectMapper objectMapper;

    List<OrderItem> fromItemsJson(Long orderFkId, String mallId, String cafe24OrderId, String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode array = objectMapper.readTree(itemsJson);
            if (!array.isArray()) {
                return List.of();
            }
            List<OrderItem> result = new ArrayList<>();
            for (JsonNode node : array) {
                result.add(toOrderItem(orderFkId, mallId, cafe24OrderId, node));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    OrderItemEntity toEntity(OrderItem domain) {
        OrderItemEntity entity = new OrderItemEntity();
        entity.setId(domain.getId());
        entity.setOrderFkId(domain.getOrderFkId());
        entity.setMallId(domain.getMallId());
        entity.setCafe24OrderId(domain.getCafe24OrderId());
        entity.setShopNo(domain.getShopNo());
        entity.setItemNo(domain.getItemNo());
        entity.setOrderItemCode(domain.getOrderItemCode());
        entity.setProductNo(domain.getProductNo());
        entity.setProductCode(domain.getProductCode());
        entity.setVariantCode(domain.getVariantCode());
        entity.setProductName(domain.getProductName());
        entity.setOptionId(domain.getOptionId());
        entity.setOptionValue(domain.getOptionValue());
        entity.setQuantity(domain.getQuantity());
        entity.setProductPrice(domain.getProductPrice());
        entity.setPaymentAmount(domain.getPaymentAmount());
        entity.setOrderStatus(domain.getOrderStatus());
        entity.setStatusCode(domain.getStatusCode());
        entity.setStatusText(domain.getStatusText());
        entity.setTrackingNo(domain.getTrackingNo());
        entity.setShippingCode(domain.getShippingCode());
        entity.setShippingCompanyName(domain.getShippingCompanyName());
        entity.setOrderedDate(domain.getOrderedDate());
        entity.setShippedDate(domain.getShippedDate());
        entity.setDeliveredDate(domain.getDeliveredDate());
        entity.setRawJson(domain.getRawJson());
        return entity;
    }

    private OrderItem toOrderItem(Long orderFkId, String mallId, String cafe24OrderId, JsonNode node) {
        return OrderItem.of(
                orderFkId,
                mallId,
                cafe24OrderId,
                intOrNull(node, "shop_no"),
                longOrNull(node, "item_no"),
                textOrNull(node, "order_item_code"),
                longOrNull(node, "product_no"),
                textOrNull(node, "product_code"),
                textOrNull(node, "variant_code"),
                textOrNull(node, "product_name"),
                textOrNull(node, "option_id"),
                textOrNull(node, "option_value"),
                intOrNull(node, "quantity"),
                decimalOrNull(node, "product_price"),
                decimalOrNull(node, "payment_amount"),
                textOrNull(node, "order_status"),
                textOrNull(node, "status_code"),
                textOrNull(node, "status_text"),
                textOrNull(node, "tracking_no"),
                textOrNull(node, "shipping_code"),
                textOrNull(node, "shipping_company_name"),
                dateTimeOrNull(node, "ordered_date"),
                dateTimeOrNull(node, "shipped_date"),
                dateTimeOrNull(node, "delivered_date"),
                node.toString()
        );
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asInt();
    }

    private Long longOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asLong();
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        try {
            return new BigDecimal(n.asText());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime dateTimeOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull()) return null;
        try {
            return OffsetDateTime.parse(n.asText()).toLocalDateTime();
        } catch (Exception e) {
            return null;
        }
    }
}
