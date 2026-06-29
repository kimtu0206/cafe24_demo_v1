package org.example.cafe24_demo_v1.benefit.infrastructure.external;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Cafe24BenefitClientTest {

    private MockRestServiceServer mockServer;
    private Cafe24BenefitClient client;

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

        client = new Cafe24BenefitClient(properties, restTemplate);
    }

    @Test
    void 파라미터_없이_호출하면_올바른_URL과_헤더로_요청한다() {
        mockServer.expect(requestTo("https://mymall.cafe24api.com/api/v2/admin/benefits"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(header("X-Cafe24-Api-Version", "2024-06-01"))
                .andRespond(withSuccess("""
                        {"benefits": [{"benefit_no": 3, "benefit_name": "Group Sale"}]}
                        """, MediaType.APPLICATION_JSON));

        List<Benefit> benefits = client.listBenefits("mymall", null, null, null, credential);

        assertThat(benefits).hasSize(1);
        assertThat(benefits.get(0).getBenefitNo()).isEqualTo(3);
        mockServer.verify();
    }

    @Test
    void useBenefit_T로_조회하면_쿼리파라미터가_포함된다() {
        mockServer.expect(requestTo(allOf(
                        containsString("https://mymall.cafe24api.com/api/v2/admin/benefits"),
                        containsString("use_benefit=T"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"benefits": [{"benefit_no": 3, "use_benefit": "T", "benefit_name": "Group Sale"}]}
                        """, MediaType.APPLICATION_JSON));

        List<Benefit> benefits = client.listBenefits("mymall", "T", null, null, credential);

        assertThat(benefits).hasSize(1);
        assertThat(benefits.get(0).getUseBenefit()).isEqualTo("T");
        mockServer.verify();
    }

    @Test
    void 기간으로_조회하면_start_end_쿼리파라미터가_모두_포함된다() {
        mockServer.expect(requestTo(allOf(
                        containsString("benefit_start_date=2024-01-01"),
                        containsString("benefit_end_date=2024-12-31"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"benefits": []}
                        """, MediaType.APPLICATION_JSON));

        List<Benefit> benefits = client.listBenefits("mymall", null, "2024-01-01", "2024-12-31", credential);

        assertThat(benefits).isEmpty();
        mockServer.verify();
    }

    @Test
    void 응답의_모든_필드를_도메인_모델로_변환한다() {
        mockServer.expect(requestTo(containsString("/benefits")))
                .andRespond(withSuccess("""
                        {"benefits": [{
                            "shop_no": 1,
                            "benefit_no": 3,
                            "use_benefit": "T",
                            "benefit_name": "Group Sale",
                            "benefit_division": "P",
                            "benefit_type": "PG",
                            "use_benefit_period": "T",
                            "benefit_start_date": "2018-12-04T00:00:00+09:00",
                            "benefit_end_date": "2018-12-04T23:55:00+09:00",
                            "platform_types": ["P", "M"],
                            "use_group_binding": "M",
                            "customer_group_list": [1, 8, 9],
                            "product_binding_type": "A",
                            "use_except_category": "T",
                            "icon_url": "https://example.com/icon.gif",
                            "available_coupon": "T",
                            "repurchase_sale": null,
                            "bulk_purchase_sale": null,
                            "member_sale": null
                        }]}
                        """, MediaType.APPLICATION_JSON));

        List<Benefit> benefits = client.listBenefits("mymall", null, null, null, credential);

        assertThat(benefits).hasSize(1);
        Benefit benefit = benefits.get(0);
        assertThat(benefit.getShopNo()).isEqualTo(1);
        assertThat(benefit.getBenefitNo()).isEqualTo(3);
        assertThat(benefit.getUseBenefit()).isEqualTo("T");
        assertThat(benefit.getBenefitName()).isEqualTo("Group Sale");
        assertThat(benefit.getBenefitDivision()).isEqualTo("P");
        assertThat(benefit.getBenefitType()).isEqualTo("PG");
        assertThat(benefit.getUseBenefitPeriod()).isEqualTo("T");
        assertThat(benefit.getBenefitStartDate()).isNotNull();
        assertThat(benefit.getBenefitEndDate()).isNotNull();
        assertThat(benefit.getPlatformTypes()).containsExactly("P", "M");
        assertThat(benefit.getCustomerGroupList()).containsExactly(1, 8, 9);
        assertThat(benefit.getProductBindingType()).isEqualTo("A");
        assertThat(benefit.getUseExceptCategory()).isEqualTo("T");
        assertThat(benefit.getIconUrl()).isEqualTo("https://example.com/icon.gif");
        assertThat(benefit.getAvailableCoupon()).isEqualTo("T");
    }

    @Test
    void 빈_결과를_반환하면_빈_리스트를_반환한다() {
        mockServer.expect(requestTo(containsString("/benefits")))
                .andRespond(withSuccess("""
                        {"benefits": []}
                        """, MediaType.APPLICATION_JSON));

        List<Benefit> benefits = client.listBenefits("mymall", null, null, null, credential);

        assertThat(benefits).isEmpty();
    }

    @Test
    void null_파라미터는_URL에_포함되지_않는다() {
        mockServer.expect(requestTo(allOf(
                        containsString("https://mymall.cafe24api.com/api/v2/admin/benefits"),
                        not(containsString("use_benefit")),
                        not(containsString("benefit_start_date")),
                        not(containsString("benefit_end_date")))))
                .andRespond(withSuccess("""
                        {"benefits": []}
                        """, MediaType.APPLICATION_JSON));

        client.listBenefits("mymall", null, null, null, credential);

        mockServer.verify();
    }

    @Test
    void Cafe24가_에러를_반환하면_Cafe24ApiException을_던진다() {
        mockServer.expect(requestTo(containsString("/benefits")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": \"invalid token\"}"));

        assertThatThrownBy(() -> client.listBenefits("mymall", null, null, null, credential))
                .isInstanceOf(Cafe24ApiException.class);
    }
}
