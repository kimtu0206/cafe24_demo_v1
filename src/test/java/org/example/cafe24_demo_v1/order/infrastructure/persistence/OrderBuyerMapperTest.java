package org.example.cafe24_demo_v1.order.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cafe24_demo_v1.order.domain.model.OrderBuyer;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBuyerMapperTest {

    private final OrderBuyerMapper mapper = new OrderBuyerMapper(new ObjectMapper());

    private static final String SAMPLE_BUYER_JSON = """
            {
              "shop_no": 1,
              "member_id": "entwizcomms",
              "member_group_no": 1,
              "name": "주식회사 에이아이소울",
              "names_furigana": "",
              "email": "jangyl@naver.com",
              "phone": "",
              "cellphone": "010-8469-5122",
              "customer_notification": null,
              "updated_date": null,
              "user_id": null,
              "user_name": null,
              "company_name": null,
              "company_registration_no": null,
              "buyer_zipcode": "18587",
              "buyer_address1": "경기 화성시 향남읍 장짐리 108-4 신성미곡처리장,대림콘테이너",
              "buyer_address2": "101호"
            }
            """;

    @Test
    void fromBuyerJson_모든_필드를_정상_파싱한다() {
        Optional<OrderBuyer> result = mapper.fromBuyerJson(10L, "testmall", "20260622-0000054", SAMPLE_BUYER_JSON);

        assertThat(result).isPresent();
        OrderBuyer buyer = result.get();

        assertThat(buyer.getOrderFkId()).isEqualTo(10L);
        assertThat(buyer.getMallId()).isEqualTo("testmall");
        assertThat(buyer.getCafe24OrderId()).isEqualTo("20260622-0000054");
        assertThat(buyer.getShopNo()).isEqualTo(1);
        assertThat(buyer.getMemberId()).isEqualTo("entwizcomms");
        assertThat(buyer.getMemberGroupNo()).isEqualTo(1);
        assertThat(buyer.getName()).isEqualTo("주식회사 에이아이소울");
        assertThat(buyer.getNamesFurigana()).isEmpty();
        assertThat(buyer.getEmail()).isEqualTo("jangyl@naver.com");
        assertThat(buyer.getPhone()).isEmpty();
        assertThat(buyer.getCellphone()).isEqualTo("010-8469-5122");
        assertThat(buyer.getCustomerNotification()).isNull();
        assertThat(buyer.getUpdatedDate()).isNull();
        assertThat(buyer.getUserId()).isNull();
        assertThat(buyer.getUserName()).isNull();
        assertThat(buyer.getCompanyName()).isNull();
        assertThat(buyer.getCompanyRegistrationNo()).isNull();
        assertThat(buyer.getBuyerZipcode()).isEqualTo("18587");
        assertThat(buyer.getBuyerAddress1()).isEqualTo("경기 화성시 향남읍 장짐리 108-4 신성미곡처리장,대림콘테이너");
        assertThat(buyer.getBuyerAddress2()).isEqualTo("101호");
        assertThat(buyer.getRawJson()).isNotNull();
    }

    @Test
    void fromBuyerJson_null이면_빈_Optional을_반환한다() {
        Optional<OrderBuyer> result = mapper.fromBuyerJson(1L, "mall", "order-1", null);
        assertThat(result).isEmpty();
    }

    @Test
    void fromBuyerJson_빈_문자열이면_빈_Optional을_반환한다() {
        Optional<OrderBuyer> result = mapper.fromBuyerJson(1L, "mall", "order-1", "");
        assertThat(result).isEmpty();
    }

    @Test
    void fromBuyerJson_배열이면_빈_Optional을_반환한다() {
        Optional<OrderBuyer> result = mapper.fromBuyerJson(1L, "mall", "order-1", "[{\"shop_no\":1}]");
        assertThat(result).isEmpty();
    }

    @Test
    void fromBuyerJson_손상된_JSON이면_빈_Optional을_반환한다() {
        Optional<OrderBuyer> result = mapper.fromBuyerJson(1L, "mall", "order-1", "{invalid}");
        assertThat(result).isEmpty();
    }

    @Test
    void fromBuyerJson_비회원_주문은_member_id가_null이다() {
        String guestBuyerJson = """
                {
                  "shop_no": 1,
                  "member_id": null,
                  "name": "홍길동",
                  "email": "guest@example.com",
                  "cellphone": "010-1234-5678",
                  "buyer_zipcode": "12345",
                  "buyer_address1": "서울시 강남구",
                  "buyer_address2": "101호"
                }
                """;

        Optional<OrderBuyer> result = mapper.fromBuyerJson(2L, "mall", "order-2", guestBuyerJson);

        assertThat(result).isPresent();
        assertThat(result.get().getMemberId()).isNull();
        assertThat(result.get().getName()).isEqualTo("홍길동");
    }

    @Test
    void fromBuyerJson_company_정보가_있으면_정상_파싱한다() {
        String corporateBuyerJson = """
                {
                  "shop_no": 1,
                  "member_id": "corp_member",
                  "name": "법인 담당자",
                  "company_name": "테스트 주식회사",
                  "company_registration_no": "123-45-67890",
                  "buyer_zipcode": "06000",
                  "buyer_address1": "서울시 강남구 테헤란로 123",
                  "buyer_address2": "10층"
                }
                """;

        Optional<OrderBuyer> result = mapper.fromBuyerJson(3L, "mall", "order-3", corporateBuyerJson);

        assertThat(result).isPresent();
        OrderBuyer buyer = result.get();
        assertThat(buyer.getCompanyName()).isEqualTo("테스트 주식회사");
        assertThat(buyer.getCompanyRegistrationNo()).isEqualTo("123-45-67890");
    }
}
