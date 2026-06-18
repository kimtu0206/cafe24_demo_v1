package org.example.cafe24_demo_v1.product.infrastructure.external;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductStatus;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Cafe24ProductClientTest {

    private MockRestServiceServer mockServer;
    private Cafe24ProductClient client;

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

        client = new Cafe24ProductClient(properties, restTemplate);
    }

    @Test
    void createProduct은_응답을_도메인_모델로_변환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/products"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(header("X-Cafe24-Api-Version", "2024-06-01"))
                .andRespond(withSuccess("""
                        {"product": {"product_no": 1, "product_name": "테스트 상품", "price": "10000.00", "supply_price": "5000.00", "display": "T", "selling": "T"}}
                        """, MediaType.APPLICATION_JSON));

        Product product = client.createProduct("mymall", "테스트 상품", new BigDecimal("10000.00"), new BigDecimal("5000.00"), credential);

        assertThat(product.getProductNo()).isEqualTo(1L);
        assertThat(product.getProductName()).isEqualTo("테스트 상품");
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        mockServer.verify();
    }

    @Test
    void Cafe24가_에러를_반환하면_Cafe24ApiException을_던진다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/products"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\": \"invalid request\"}"));

        assertThatThrownBy(() ->
                client.createProduct("mymall", "테스트 상품", new BigDecimal("10000"), new BigDecimal("5000"), credential)
        ).isInstanceOf(Cafe24ApiException.class);
    }

    @Test
    void updateProduct은_응답을_도메인_모델로_변환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/products/1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(header("X-Cafe24-Api-Version", "2024-06-01"))
                .andRespond(withSuccess("""
                        {"product": {"product_no": 1, "product_name": "수정된 상품", "price": "20000.00", "supply_price": "9000.00", "display": "T", "selling": "T"}}
                        """, MediaType.APPLICATION_JSON));

        Product product = client.updateProduct("mymall", 1L, "수정된 상품", new BigDecimal("20000.00"), new BigDecimal("9000.00"), credential);

        assertThat(product.getProductNo()).isEqualTo(1L);
        assertThat(product.getProductName()).isEqualTo("수정된 상품");
        mockServer.verify();
    }

    @Test
    void deleteProduct은_DELETE_요청을_보낸다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/products/1?shop_no=1"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess());

        client.deleteProduct("mymall", 1L, credential);

        mockServer.verify();
    }
}
