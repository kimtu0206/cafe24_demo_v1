package org.example.cafe24_demo_v1.carrier.infrastructure.external;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.model.ShippingType;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Cafe24CarrierClientTest {

    private MockRestServiceServer mockServer;
    private Cafe24CarrierClient client;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        Cafe24Properties properties = new Cafe24Properties();
        properties.setMallId("mymall");
        properties.setApiVersion("2024-06-01");

        client = new Cafe24CarrierClient(properties, restTemplate);
    }

    @Test
    void Cafe24가_에러를_반환하면_Cafe24ApiException을_던진다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/carriers?offset=0&limit=100"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\": \"invalid request\"}"));

        assertThatThrownBy(() ->
                client.getCarriers("mymall", 0, 100, credential)
        ).isInstanceOf(Cafe24ApiException.class);
    }

    @Test
    void getCarriers는_응답을_도메인_모델_목록으로_변환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/carriers?offset=0&limit=100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess("""
                        {"carriers": [
                            {"carrier_id": 65, "shipping_carrier_code": "01", "shipping_carrier": "우체국",
                             "default_shipping_fee": "12000", "shipping_type": "A",
                             "default_shipping_carrier": "T", "shipping_fee_setting": "F"},
                            {"carrier_id": 66, "shipping_carrier_code": "02", "shipping_carrier": "CJ대한통운",
                             "default_shipping_fee": "3000", "shipping_type": "B",
                             "default_shipping_carrier": "F", "shipping_fee_setting": "T"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Carrier> carriers = client.getCarriers("mymall", 0, 100, credential);

        assertThat(carriers).hasSize(2);
        assertThat(carriers.get(0).getShippingCarrierCode()).isEqualTo("01");
        assertThat(carriers.get(1).getShippingCarrierCode()).isEqualTo("02");
        mockServer.verify();
    }

    @Test
    void getCarriers는_빈_목록이면_빈_리스트를_반환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/carriers?offset=0&limit=100"))
                .andRespond(withSuccess("{\"carriers\": []}", MediaType.APPLICATION_JSON));

        List<Carrier> carriers = client.getCarriers("mymall", 0, 100, credential);

        assertThat(carriers).isEmpty();
        mockServer.verify();
    }

    @Test
    void createCarrier는_shop_no와_request로_감싸서_요청하고_응답을_도메인_모델로_변환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/carriers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(content().json("""
                        {"shop_no": 1, "request": {
                            "shipping_carrier_code": "0022", "shipping_carrier": null,
                            "contact": "02-0000-0000", "secondary_contact": "02-0000-0000",
                            "email": "sample@sample.com", "homepage_url": "sample.sample.com",
                            "shipping_fee_setting": "F"
                        }}
                        """))
                .andRespond(withSuccess("""
                        {"carrier": {
                            "carrier_id": 4, "shipping_carrier_code": "0022", "shipping_carrier": "FASTBOX",
                            "contact": "02-0000-0000", "secondary_contact": "02-0000-0000",
                            "email": "sample@sample.com", "homepage_url": "sample.sample.com",
                            "shipping_fee_setting": "F",
                            "shipping_fee_setting_detail": {"shipping_type": "B"}
                        }}
                        """, MediaType.APPLICATION_JSON));

        Carrier created = client.createCarrier(
                "mymall", "0022", "02-0000-0000", "02-0000-0000", "sample@sample.com",
                null, "sample.sample.com", null, credential
        );

        assertThat(created.getCarrierId()).isEqualTo(4L);
        assertThat(created.getShippingCarrierCode()).isEqualTo("0022");
        assertThat(created.getShippingCarrierName()).isEqualTo("FASTBOX");
        assertThat(created.getShippingType()).isEqualTo(ShippingType.DOMESTIC_AND_INTERNATIONAL);
        assertThat(created.isDefaultCarrier()).isFalse();
        assertThat(created.isShippingFeeSetting()).isFalse();
        mockServer.verify();
    }

    @Test
    void createCarrier는_shipping_fee_setting_detail이_없으면_ShippingType을_NOT_SET으로_변환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/carriers"))
                .andRespond(withSuccess("""
                        {"carrier": {
                            "carrier_id": 5, "shipping_carrier_code": "0023", "shipping_carrier": "우체국",
                            "shipping_fee_setting": "F"
                        }}
                        """, MediaType.APPLICATION_JSON));

        Carrier created = client.createCarrier(
                "mymall", "0023", null, null, null, null, null, null, credential
        );

        assertThat(created.getShippingType()).isEqualTo(ShippingType.NOT_SET);
        mockServer.verify();
    }

    @Test
    void createCarrier에서_Cafe24가_에러를_반환하면_Cafe24ApiException을_던진다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/carriers"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\": \"invalid shipping_carrier_code\"}"));

        assertThatThrownBy(() ->
                client.createCarrier("mymall", "9999", null, null, null, null, null, null, credential)
        ).isInstanceOf(Cafe24ApiException.class);
    }
}
