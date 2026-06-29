package org.example.cafe24_demo_v1.member.presentation;

import org.example.cafe24_demo_v1.member.application.service.MemberService;
import org.example.cafe24_demo_v1.member.domain.model.Member;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MemberService memberService;
    @MockitoBean private Cafe24Properties cafe24Properties;

    @Test
    void member_id와_cellphone_모두_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/members"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void member_id로_검색하면_회원_목록을_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Member member = new Member(1, "sampleid", "홍길동", 1, "T", "F", "", null, "F", "F", "T", "0.00", "0.00", "0.00", "F", "0.00", "F", "M", "test@test.com", "01012345678", null, "19900101", null, null);
        given(memberService.search("mymall", "sampleid", null)).willReturn(List.of(member));

        mockMvc.perform(get("/members").param("memberId", "sampleid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.members[0].memberId").value("sampleid"))
                .andExpect(jsonPath("$.members[0].memberName").value("홍길동"));
    }

    @Test
    void cellphone으로_검색하면_회원_목록을_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Member member = new Member(1, "testid", "김철수", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, "01012345678", null, null, null, null);
        given(memberService.search("mymall", null, "01012345678")).willReturn(List.of(member));

        mockMvc.perform(get("/members").param("cellphone", "01012345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.members[0].cellphone").value("01012345678"));
    }

    @Test
    void 검색_결과가_없으면_빈_목록을_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(memberService.search(any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/members").param("memberId", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.members").isEmpty());
    }

    @Test
    void Cafe24_API_오류는_502로_변환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(memberService.search(any(), any(), any()))
                .willThrow(new Cafe24ApiException("failed", HttpStatus.INTERNAL_SERVER_ERROR, "{}", null));

        mockMvc.perform(get("/members").param("memberId", "testid"))
                .andExpect(status().isBadGateway());
    }
}
