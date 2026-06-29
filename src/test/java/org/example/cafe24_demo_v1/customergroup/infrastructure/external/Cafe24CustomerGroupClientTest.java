package org.example.cafe24_demo_v1.customergroup.infrastructure.external;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.customergroup.domain.model.CustomerGroup;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Cafe24CustomerGroupClientTest {

    private MockRestServiceServer mockServer;
    private Cafe24CustomerGroupClient client;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer",
            LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        Cafe24Properties properties = new Cafe24Properties();
        properties.setMallId("mymall");
        properties.setApiVersion("2024-06-01");

        client = new Cafe24CustomerGroupClient(properties, restTemplate);
    }

    @Test
    void 올바른_URL과_헤더로_요청한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/customergroups"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(header("X-Cafe24-Api-Version", "2024-06-01"))
                .andRespond(withSuccess("""
                        {"customergroups": [{"group_no": 1, "group_name": "일반회원"}]}
                        """, MediaType.APPLICATION_JSON));

        List<CustomerGroup> groups = client.listCustomerGroups("mymall", credential);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getGroupNo()).isEqualTo(1);
        mockServer.verify();
    }

    @Test
    void 응답의_모든_필드를_도메인_모델로_변환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/customergroups"))
                .andRespond(withSuccess("""
                        {"customergroups": [{
                            "group_no": 2,
                            "group_name": "VIP",
                            "group_description": "VIP 등급",
                            "is_buyer": "T",
                            "is_primary": "F"
                        }]}
                        """, MediaType.APPLICATION_JSON));

        List<CustomerGroup> groups = client.listCustomerGroups("mymall", credential);

        assertThat(groups).hasSize(1);
        CustomerGroup group = groups.get(0);
        assertThat(group.getGroupNo()).isEqualTo(2);
        assertThat(group.getGroupName()).isEqualTo("VIP");
        assertThat(group.getGroupDescription()).isEqualTo("VIP 등급");
        assertThat(group.getIsBuyer()).isEqualTo("T");
        assertThat(group.getIsPrimary()).isEqualTo("F");
    }

    @Test
    void 빈_결과를_반환하면_빈_리스트를_반환한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/customergroups"))
                .andRespond(withSuccess("""
                        {"customergroups": []}
                        """, MediaType.APPLICATION_JSON));

        List<CustomerGroup> groups = client.listCustomerGroups("mymall", credential);

        assertThat(groups).isEmpty();
    }

    @Test
    void Cafe24가_에러를_반환하면_Cafe24ApiException을_던진다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/customergroups"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": \"invalid token\"}"));

        assertThatThrownBy(() -> client.listCustomerGroups("mymall", credential))
                .isInstanceOf(Cafe24ApiException.class);
    }
}
