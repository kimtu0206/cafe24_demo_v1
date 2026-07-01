package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cafe24_demo_v1.order.domain.model.OrderReceiver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderReceiverMapperTest {

    private final OrderReceiverMapper mapper = new OrderReceiverMapper(new ObjectMapper());

    private static final String SAMPLE_RECEIVERS_JSON = """
            [
              {
                "shop_no": 1,
                "name": "JIANG YINLONG",
                "name_furigana": "",
                "phone": "",
                "cellphone": "010-8469-5122",
                "virtual_phone_no": null,
                "zipcode": "18587",
                "address1": "경기 화성시 향남읍 장짐리 108-4",
                "address2": "101호",
                "address_state": "",
                "address_city": "",
                "address_street": "",
                "address_full": "경기 화성시 향남읍 장짐리 108-4 101호",
                "name_en": "",
                "city_en": null,
                "state_en": null,
                "street_en": null,
                "country_code": null,
                "country_name": null,
                "country_name_en": null,
                "shipping_message": "부재 시 문 앞에 놓아주세요.",
                "clearance_information_type": null,
                "clearance_information": null,
                "wished_delivery_date": "",
                "wished_delivery_time": null,
                "shipping_code": "D-20260622-0000054-00"
              }
            ]
            """;

    @Test
    void fromReceiversJson_모든_필드를_정상_파싱한다() {
        List<OrderReceiver> receivers = mapper.fromReceiversJson(10L, "testmall", "20260622-0000054", SAMPLE_RECEIVERS_JSON);

        assertThat(receivers).hasSize(1);
        OrderReceiver r = receivers.get(0);

        assertThat(r.getOrderFkId()).isEqualTo(10L);
        assertThat(r.getMallId()).isEqualTo("testmall");
        assertThat(r.getCafe24OrderId()).isEqualTo("20260622-0000054");
        assertThat(r.getShopNo()).isEqualTo(1);
        assertThat(r.getName()).isEqualTo("JIANG YINLONG");
        assertThat(r.getNameFurigana()).isEmpty();
        assertThat(r.getPhone()).isEmpty();
        assertThat(r.getCellphone()).isEqualTo("010-8469-5122");
        assertThat(r.getVirtualPhoneNo()).isNull();
        assertThat(r.getZipcode()).isEqualTo("18587");
        assertThat(r.getAddress1()).isEqualTo("경기 화성시 향남읍 장짐리 108-4");
        assertThat(r.getAddress2()).isEqualTo("101호");
        assertThat(r.getAddressFull()).isEqualTo("경기 화성시 향남읍 장짐리 108-4 101호");
        assertThat(r.getShippingMessage()).isEqualTo("부재 시 문 앞에 놓아주세요.");
        assertThat(r.getCountryCode()).isNull();
        assertThat(r.getShippingCode()).isEqualTo("D-20260622-0000054-00");
        assertThat(r.getRawJson()).isNotNull();
    }

    @Test
    void fromReceiversJson_receivers가_null이면_빈_리스트를_반환한다() {
        List<OrderReceiver> receivers = mapper.fromReceiversJson(1L, "mall", "order-1", null);
        assertThat(receivers).isEmpty();
    }

    @Test
    void fromReceiversJson_receivers가_빈_문자열이면_빈_리스트를_반환한다() {
        List<OrderReceiver> receivers = mapper.fromReceiversJson(1L, "mall", "order-1", "");
        assertThat(receivers).isEmpty();
    }

    @Test
    void fromReceiversJson_빈_배열이면_빈_리스트를_반환한다() {
        List<OrderReceiver> receivers = mapper.fromReceiversJson(1L, "mall", "order-1", "[]");
        assertThat(receivers).isEmpty();
    }

    @Test
    void fromReceiversJson_여러_수령자이면_모두_파싱한다() {
        String json = """
                [
                  {"shop_no": 1, "name": "수령자A", "shipping_code": "D-001"},
                  {"shop_no": 1, "name": "수령자B", "shipping_code": "D-002"}
                ]
                """;

        List<OrderReceiver> receivers = mapper.fromReceiversJson(5L, "mall", "order-multi", json);

        assertThat(receivers).hasSize(2);
        assertThat(receivers.get(0).getName()).isEqualTo("수령자A");
        assertThat(receivers.get(1).getName()).isEqualTo("수령자B");
        assertThat(receivers).allMatch(r -> r.getOrderFkId().equals(5L));
    }
}
