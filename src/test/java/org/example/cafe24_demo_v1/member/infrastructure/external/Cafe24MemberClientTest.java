package org.example.cafe24_demo_v1.member.infrastructure.external;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.member.domain.model.Member;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class Cafe24MemberClientTest {

    private MockRestServiceServer mockServer;
    private Cafe24MemberClient client;

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

        client = new Cafe24MemberClient(properties, restTemplate);
    }

    @Test
    void member_id로_검색하면_올바른_URL과_헤더로_요청한다() {
        mockServer.expect(requestTo(allOf(
                        containsString("https://mymall.cafe24api.com/api/v2/admin/customers"),
                        containsString("member_id=sampleid"))))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(header("X-Cafe24-Api-Version", "2024-06-01"))
                .andRespond(withSuccess("""
                        {"customers": [{"member_id": "sampleid", "member_name": "홍길동"}, {"member_id": "testid", "member_name": "김철수"}]}
                        """, MediaType.APPLICATION_JSON));

        List<Member> members = client.searchMembers("mymall", "sampleid,testid", null, credential);

        assertThat(members).hasSize(2);
        assertThat(members.get(0).getMemberId()).isEqualTo("sampleid");
        assertThat(members.get(1).getMemberId()).isEqualTo("testid");
        mockServer.verify();
    }

    @Test
    void cellphone으로_검색하면_올바른_URL로_요청한다() {
        mockServer.expect(requestTo(allOf(
                        containsString("https://mymall.cafe24api.com/api/v2/admin/customers"),
                        containsString("cellphone=01012345678"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"customers": [{"member_id": "testid", "cellphone": "01012345678"}]}
                        """, MediaType.APPLICATION_JSON));

        List<Member> members = client.searchMembers("mymall", null, "01012345678", credential);

        assertThat(members).hasSize(1);
        assertThat(members.get(0).getCellphone()).isEqualTo("01012345678");
        mockServer.verify();
    }

    @Test
    void 응답의_모든_필드를_도메인_모델로_변환한다() {
        mockServer.expect(requestTo(containsString("/customers")))
                .andRespond(withSuccess("""
                        {"customers": [{"shop_no": 1, "member_id": "ppiyong", "group_no": 5, "member_authentication": "T", "use_blacklist": "F", "blacklist_type": "", "authentication_method": null, "sms": "F", "news_mail": "F", "solar_calendar": "T", "total_points": "0.00", "available_points": "0.00", "used_points": "0.00", "last_login_date": "2026-05-27T15:05:41+09:00", "gender": "", "use_mobile_app": "F", "available_credits": "0.00", "created_date": "2026-05-21T16:23:41+09:00", "fixed_group": "F"}]}
                        """, MediaType.APPLICATION_JSON));

        List<Member> members = client.searchMembers("mymall", "ppiyong", null, credential);

        assertThat(members).hasSize(1);
        Member member = members.get(0);
        assertThat(member.getShopNo()).isEqualTo(1);
        assertThat(member.getMemberId()).isEqualTo("ppiyong");
        assertThat(member.getGroupNo()).isEqualTo(5);
        assertThat(member.getMemberAuthentication()).isEqualTo("T");
        assertThat(member.getUseBlacklist()).isEqualTo("F");
        assertThat(member.getSms()).isEqualTo("F");
        assertThat(member.getNewsMail()).isEqualTo("F");
        assertThat(member.getSolarCalendar()).isEqualTo("T");
        assertThat(member.getTotalPoints()).isEqualTo("0.00");
        assertThat(member.getAvailablePoints()).isEqualTo("0.00");
        assertThat(member.getUsedPoints()).isEqualTo("0.00");
        assertThat(member.getUseMobileApp()).isEqualTo("F");
        assertThat(member.getAvailableCredits()).isEqualTo("0.00");
        assertThat(member.getFixedGroup()).isEqualTo("F");
        assertThat(member.getCreatedDate()).isNotNull();
        assertThat(member.getLastLoginDate()).isNotNull();
    }

    @Test
    void member_id_쉼표_뒤_공백은_제거하고_요청한다() {
        mockServer.expect(requestTo(allOf(
                        containsString("https://mymall.cafe24api.com/api/v2/admin/customers"),
                        containsString("member_id=aisoul0002,ppiyong"))))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"customers": [{"member_id": "aisoul0002"}, {"member_id": "ppiyong"}]}
                        """, MediaType.APPLICATION_JSON));

        List<Member> members = client.searchMembers("mymall", "aisoul0002, ppiyong", null, credential);

        assertThat(members).hasSize(2);
        mockServer.verify();
    }

    @Test
    void 빈_결과를_반환하면_빈_리스트를_반환한다() {
        mockServer.expect(requestTo(containsString("/customers")))
                .andRespond(withSuccess("""
                        {"customers": []}
                        """, MediaType.APPLICATION_JSON));

        List<Member> members = client.searchMembers("mymall", "nonexistent", null, credential);

        assertThat(members).isEmpty();
    }

    @Test
    void Cafe24가_에러를_반환하면_Cafe24ApiException을_던진다() {
        mockServer.expect(requestTo(containsString("/customers")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"error\": \"invalid parameter\"}"));

        assertThatThrownBy(() -> client.searchMembers("mymall", "testid", null, credential))
                .isInstanceOf(Cafe24ApiException.class);
    }
}
