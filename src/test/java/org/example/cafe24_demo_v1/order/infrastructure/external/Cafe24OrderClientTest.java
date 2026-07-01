package org.example.cafe24_demo_v1.order.infrastructure.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.order.domain.model.Order;
import org.example.cafe24_demo_v1.order.domain.model.OrderType;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Cafe24OrderClientTest {

    private MockRestServiceServer mockServer;
    private Cafe24OrderClient client;

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

        client = new Cafe24OrderClient(properties, restTemplate, new ObjectMapper());
    }

    @Test
    void getOrders는_start_date와_end_date_파라미터를_yyyy_MM_dd_형식으로_보낸다() {
        LocalDateTime updatedSince = LocalDateTime.of(2017, 1, 1, 0, 0);
        mockServer.expect(requestTo(startsWith(
                        "https://mymall.cafe24api.com/api/v2/admin/orders?start_date=2017-01-01&end_date=")))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"orders\": []}", MediaType.APPLICATION_JSON));

        client.getOrders("mymall", updatedSince, 0, 100, credential);

        mockServer.verify();
    }

    @Test
    void getOrders는_하위_리소스를_함께_조회하기_위해_embed_파라미터를_보낸다() {
        LocalDateTime updatedSince = LocalDateTime.of(2017, 1, 1, 0, 0);
        mockServer.expect(requestTo(containsString(
                        "embed=items,receivers,buyer,return,cancellation,exchange")))
                .andExpect(method(GET))
                .andRespond(withSuccess("{\"orders\": []}", MediaType.APPLICATION_JSON));

        client.getOrders("mymall", updatedSince, 0, 100, credential);

        mockServer.verify();
    }

    @Test
    void getOrders는_응답을_도메인_모델로_변환하고_원본을_보존한다() {
        // 실제 Cafe24 GET /admin/orders 응답 형태(주요 필드만 추림): order_status 없음, payment_method는 배열,
        // 결제금액은 최상위 payment_amount, 구매자 이메일은 member_email
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"orders": [
                          {
                            "order_id": "20170710-0000013",
                            "member_id": "sampleid",
                            "member_email": "sample@sample.com",
                            "payment_amount": "30000.00",
                            "payment_method": ["card", "cash"],
                            "paid": "T",
                            "canceled": "F",
                            "order_date": "2018-07-04T11:21:35+09:00"
                          }
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Order> orders = client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential);

        assertThat(orders).hasSize(1);
        Order order = orders.get(0);
        assertThat(order.getMallId()).isEqualTo("mymall");
        assertThat(order.getOrderId()).isEqualTo("20170710-0000013");
        assertThat(order.getOrderStatus()).isNull(); // 응답에 order_status가 없어 비즈니스 규칙 정해지기 전까지 null
        assertThat(order.getOrderType()).isEqualTo(OrderType.MEMBER);
        assertThat(order.getBuyerName()).isNull();   // 개인정보 embed 미적용으로 null
        assertThat(order.getBuyerEmail()).isEqualTo("sample@sample.com");
        assertThat(order.getTotalAmount()).isEqualTo(new BigDecimal("30000.00"));
        assertThat(order.getPaymentMethod()).isEqualTo("card,cash");
        assertThat(order.getOrderedAt()).isEqualTo(OffsetDateTime.parse("2018-07-04T11:21:35+09:00").toLocalDateTime());
        assertThat(order.getRawJson()).contains("20170710-0000013");
        mockServer.verify();
    }

    @Test
    void getOrders는_embed로_받은_하위_리소스를_각각_같은_이름의_필드에_담는다() {
        // embed=items,receivers,buyer,return,cancellation,exchange 응답 시 주문 객체 안에 하위 리소스가 함께 포함된다.
        // 정확한 하위 필드 구조와 무관하게, 리소스 이름과 같은 필드에 원본 그대로 담기는지만 확인한다.
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andRespond(withSuccess("""
                        {"orders": [
                          {
                            "order_id": "20170710-0000013",
                            "member_id": "sampleid",
                            "member_email": "sample@sample.com",
                            "payment_amount": "30000.00",
                            "payment_method": ["card"],
                            "order_date": "2018-07-04T11:21:35+09:00",
                            "items": [
                              {"item_no": 1, "product_name": "샘플 상품"}
                            ],
                            "buyer": {"name": "홍길동"},
                            "receivers": [
                              {"receiver_name": "홍길동", "receiver_phone": "010-0000-0000"}
                            ],
                            "return": {"return_no": "1"},
                            "cancellation": {"cancel_no": "1"},
                            "exchange": {"exchange_no": "1"}
                          }
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Order> orders = client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential);

        assertThat(orders).hasSize(1);
        Order order = orders.get(0);
        assertThat(order.getItems()).contains("샘플 상품");
        assertThat(order.getReceivers()).contains("홍길동", "receiver_phone");
        assertThat(order.getBuyer()).contains("홍길동");
        assertThat(order.getReturnInfo()).contains("return_no");
        assertThat(order.getCancellation()).contains("cancel_no");
        assertThat(order.getExchange()).contains("exchange_no");
        assertThat(order.getRawJson()).contains("샘플 상품", "receiver_phone", "return_no");
        mockServer.verify();
    }

    @Test
    void member_id가_없으면_GUEST로_분류된다() {
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"orders": [
                          {
                            "order_id": "20170710-0000099",
                            "member_email": "",
                            "payment_amount": "15000.00",
                            "payment_method": ["card"],
                            "order_date": "2018-07-04T11:21:35+09:00"
                          }
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Order> orders = client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential);

        assertThat(orders).hasSize(1);
        Order order = orders.get(0);
        assertThat(order.getOrderType()).isEqualTo(OrderType.GUEST);
        assertThat(order.getMemberId()).isNull();
        mockServer.verify();
    }

    @Test
    void getOrders는_embed_응답에_하위_리소스가_없으면_해당_필드를_null로_둔다() {
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andRespond(withSuccess("""
                        {"orders": [
                          {
                            "order_id": "20170710-0000013",
                            "member_email": "sample@sample.com",
                            "payment_amount": "30000.00",
                            "payment_method": ["card"],
                            "order_date": "2018-07-04T11:21:35+09:00"
                          }
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Order> orders = client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential);

        Order order = orders.get(0);
        assertThat(order.getItems()).isNull();
        assertThat(order.getReceivers()).isNull();
        assertThat(order.getBuyer()).isNull();
        assertThat(order.getReturnInfo()).isNull();
        assertThat(order.getCancellation()).isNull();
        assertThat(order.getExchange()).isNull();
    }

    @Test
    void 주문이_없으면_빈_목록을_반환한다() {
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andRespond(withSuccess("{\"orders\": []}", MediaType.APPLICATION_JSON));

        List<Order> orders = client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential);

        assertThat(orders).isEmpty();
    }

    @Test
    void Cafe24가_에러를_반환하면_Cafe24ApiException을_던진다() {
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\": \"invalid request\"}"));

        assertThatThrownBy(() ->
                client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential)
        ).isInstanceOf(Cafe24ApiException.class);
    }

    @Test
    void getOrders는_canceled와_cancel_date를_도메인_모델에_매핑한다() {
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andRespond(withSuccess("""
                        {"orders": [
                          {
                            "order_id": "20260701-0000033",
                            "member_id": "sampleid",
                            "member_email": "sample@sample.com",
                            "payment_amount": "0.00",
                            "payment_method": ["cash"],
                            "order_date": "2026-07-01T13:29:17+09:00",
                            "canceled": "T",
                            "cancel_date": "2026-07-01T13:30:15+09:00"
                          }
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Order> orders = client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential);

        assertThat(orders).hasSize(1);
        Order order = orders.get(0);
        assertThat(order.getCanceled()).isEqualTo("T");
        assertThat(order.getCancelDate()).isEqualTo(OffsetDateTime.parse("2026-07-01T13:30:15+09:00").toLocalDateTime());
        mockServer.verify();
    }

    @Test
    void getOrders는_취소되지_않은_주문의_cancel_date를_null로_둔다() {
        mockServer.expect(requestTo(startsWith("https://mymall.cafe24api.com/api/v2/admin/orders?")))
                .andRespond(withSuccess("""
                        {"orders": [
                          {
                            "order_id": "20260701-0000001",
                            "member_email": "sample@sample.com",
                            "payment_amount": "30000.00",
                            "payment_method": ["card"],
                            "order_date": "2026-07-01T10:00:00+09:00",
                            "canceled": "F",
                            "cancel_date": null
                          }
                        ]}
                        """, MediaType.APPLICATION_JSON));

        List<Order> orders = client.getOrders("mymall", LocalDateTime.now().minusMinutes(10), 0, 100, credential);

        Order order = orders.get(0);
        assertThat(order.getCanceled()).isEqualTo("F");
        assertThat(order.getCancelDate()).isNull();
        mockServer.verify();
    }

    @Test
    void getOrder는_단건_엔드포인트로_주문을_조회한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/orders/20170710-0000013"
                        + "?embed=items,receivers,buyer,return,cancellation,exchange"))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                        {"order": {
                            "order_id": "20170710-0000013",
                            "member_id": "sampleid",
                            "member_email": "sample@sample.com",
                            "payment_amount": "30000.00",
                            "payment_method": ["card"],
                            "order_date": "2018-07-04T11:21:35+09:00",
                            "cancellation": [{"cancel_no": "1", "status": "F"}]
                        }}
                        """, MediaType.APPLICATION_JSON));

        Optional<Order> order = client.getOrder("mymall", "20170710-0000013", credential);

        assertThat(order).isPresent();
        assertThat(order.get().getOrderId()).isEqualTo("20170710-0000013");
        assertThat(order.get().getPaymentMethod()).isEqualTo("card");
        assertThat(order.get().getCancellation()).contains("cancel_no");
    }

    @Test
    void getOrder는_404이면_빈_Optional을_반환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/orders/missing"
                        + "?embed=items,receivers,buyer,return,cancellation,exchange"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).body("{\"error\": \"not found\"}"));

        Optional<Order> order = client.getOrder("mymall", "missing", credential);

        assertThat(order).isEmpty();
    }
}
