package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderBuyer;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
class OrderBuyerMapper {

    private final ObjectMapper objectMapper;

    Optional<OrderBuyer> fromBuyerJson(Long orderFkId, String mallId, String cafe24OrderId, String buyerJson) {
        if (buyerJson == null || buyerJson.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(buyerJson);
            if (node == null || node.isNull() || !node.isObject()) {
                return Optional.empty();
            }
            return Optional.of(toOrderBuyer(orderFkId, mallId, cafe24OrderId, node));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    OrderBuyerEntity toEntity(OrderBuyer domain) {
        OrderBuyerEntity entity = new OrderBuyerEntity();
        entity.setId(domain.getId());
        entity.setOrderFkId(domain.getOrderFkId());
        entity.setMallId(domain.getMallId());
        entity.setCafe24OrderId(domain.getCafe24OrderId());
        entity.setShopNo(domain.getShopNo());
        entity.setMemberId(domain.getMemberId());
        entity.setMemberGroupNo(domain.getMemberGroupNo());
        entity.setName(domain.getName());
        entity.setNamesFurigana(domain.getNamesFurigana());
        entity.setEmail(domain.getEmail());
        entity.setPhone(domain.getPhone());
        entity.setCellphone(domain.getCellphone());
        entity.setCustomerNotification(domain.getCustomerNotification());
        entity.setUpdatedDate(domain.getUpdatedDate());
        entity.setUserId(domain.getUserId());
        entity.setUserName(domain.getUserName());
        entity.setCompanyName(domain.getCompanyName());
        entity.setCompanyRegistrationNo(domain.getCompanyRegistrationNo());
        entity.setBuyerZipcode(domain.getBuyerZipcode());
        entity.setBuyerAddress1(domain.getBuyerAddress1());
        entity.setBuyerAddress2(domain.getBuyerAddress2());
        entity.setRawJson(domain.getRawJson());
        return entity;
    }

    private OrderBuyer toOrderBuyer(Long orderFkId, String mallId, String cafe24OrderId, JsonNode node) {
        return OrderBuyer.of(
                orderFkId,
                mallId,
                cafe24OrderId,
                intOrNull(node, "shop_no"),
                textOrNull(node, "member_id"),
                intOrNull(node, "member_group_no"),
                textOrNull(node, "name"),
                textOrNull(node, "names_furigana"),
                textOrNull(node, "email"),
                textOrNull(node, "phone"),
                textOrNull(node, "cellphone"),
                textOrNull(node, "customer_notification"),
                textOrNull(node, "updated_date"),
                textOrNull(node, "user_id"),
                textOrNull(node, "user_name"),
                textOrNull(node, "company_name"),
                textOrNull(node, "company_registration_no"),
                textOrNull(node, "buyer_zipcode"),
                textOrNull(node, "buyer_address1"),
                textOrNull(node, "buyer_address2"),
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
}
