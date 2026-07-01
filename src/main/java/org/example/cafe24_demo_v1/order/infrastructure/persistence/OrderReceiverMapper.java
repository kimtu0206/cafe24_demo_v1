package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.order.domain.model.OrderReceiver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
class OrderReceiverMapper {

    private final ObjectMapper objectMapper;

    List<OrderReceiver> fromReceiversJson(Long orderFkId, String mallId, String cafe24OrderId, String receiversJson) {
        if (receiversJson == null || receiversJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode array = objectMapper.readTree(receiversJson);
            if (!array.isArray()) {
                return List.of();
            }
            List<OrderReceiver> result = new ArrayList<>();
            for (JsonNode node : array) {
                result.add(toOrderReceiver(orderFkId, mallId, cafe24OrderId, node));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    OrderReceiverEntity toEntity(OrderReceiver domain) {
        OrderReceiverEntity entity = new OrderReceiverEntity();
        entity.setId(domain.getId());
        entity.setOrderFkId(domain.getOrderFkId());
        entity.setMallId(domain.getMallId());
        entity.setCafe24OrderId(domain.getCafe24OrderId());
        entity.setShopNo(domain.getShopNo());
        entity.setName(domain.getName());
        entity.setNameFurigana(domain.getNameFurigana());
        entity.setPhone(domain.getPhone());
        entity.setCellphone(domain.getCellphone());
        entity.setVirtualPhoneNo(domain.getVirtualPhoneNo());
        entity.setZipcode(domain.getZipcode());
        entity.setAddress1(domain.getAddress1());
        entity.setAddress2(domain.getAddress2());
        entity.setAddressState(domain.getAddressState());
        entity.setAddressCity(domain.getAddressCity());
        entity.setAddressStreet(domain.getAddressStreet());
        entity.setAddressFull(domain.getAddressFull());
        entity.setNameEn(domain.getNameEn());
        entity.setCityEn(domain.getCityEn());
        entity.setStateEn(domain.getStateEn());
        entity.setStreetEn(domain.getStreetEn());
        entity.setCountryCode(domain.getCountryCode());
        entity.setCountryName(domain.getCountryName());
        entity.setCountryNameEn(domain.getCountryNameEn());
        entity.setShippingMessage(domain.getShippingMessage());
        entity.setClearanceInformationType(domain.getClearanceInformationType());
        entity.setClearanceInformation(domain.getClearanceInformation());
        entity.setWishedDeliveryDate(domain.getWishedDeliveryDate());
        entity.setWishedDeliveryTime(domain.getWishedDeliveryTime());
        entity.setShippingCode(domain.getShippingCode());
        entity.setRawJson(domain.getRawJson());
        return entity;
    }

    private OrderReceiver toOrderReceiver(Long orderFkId, String mallId, String cafe24OrderId, JsonNode node) {
        return OrderReceiver.of(
                orderFkId,
                mallId,
                cafe24OrderId,
                intOrNull(node, "shop_no"),
                textOrNull(node, "name"),
                textOrNull(node, "name_furigana"),
                textOrNull(node, "phone"),
                textOrNull(node, "cellphone"),
                textOrNull(node, "virtual_phone_no"),
                textOrNull(node, "zipcode"),
                textOrNull(node, "address1"),
                textOrNull(node, "address2"),
                textOrNull(node, "address_state"),
                textOrNull(node, "address_city"),
                textOrNull(node, "address_street"),
                textOrNull(node, "address_full"),
                textOrNull(node, "name_en"),
                textOrNull(node, "city_en"),
                textOrNull(node, "state_en"),
                textOrNull(node, "street_en"),
                textOrNull(node, "country_code"),
                textOrNull(node, "country_name"),
                textOrNull(node, "country_name_en"),
                textOrNull(node, "shipping_message"),
                textOrNull(node, "clearance_information_type"),
                textOrNull(node, "clearance_information"),
                textOrNull(node, "wished_delivery_date"),
                textOrNull(node, "wished_delivery_time"),
                textOrNull(node, "shipping_code"),
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
