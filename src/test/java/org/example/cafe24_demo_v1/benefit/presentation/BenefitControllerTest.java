package org.example.cafe24_demo_v1.benefit.presentation;

import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
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

@WebMvcTest(BenefitController.class)
class BenefitControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BenefitService benefitService;
    @MockitoBean private Cafe24Properties cafe24Properties;

    @Test
    void 파라미터_없이_전체_혜택을_조회한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Benefit benefit = new Benefit(1, 3, "T", "Group Sale", "P", "PG", "T", null, null, List.of("P", "M"), "M", List.of(1, 8, 9), "A", "T", null, "T", null, null, null);
        given(benefitService.list("mymall", null, null, null)).willReturn(List.of(benefit));

        mockMvc.perform(get("/benefits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.benefits[0].benefitNo").value(3))
                .andExpect(jsonPath("$.benefits[0].benefitName").value("Group Sale"));
    }

    @Test
    void useBenefit_T로_진행중_혜택만_조회한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Benefit benefit = new Benefit(1, 3, "T", "Group Sale", "P", "PG", "T", null, null, List.of(), "N", List.of(), "A", "T", null, "T", null, null, null);
        given(benefitService.list("mymall", "T", null, null)).willReturn(List.of(benefit));

        mockMvc.perform(get("/benefits").param("useBenefit", "T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.benefits[0].useBenefit").value("T"));
    }

    @Test
    void 기간으로_조회하면_서비스에_파라미터가_전달된다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(benefitService.list("mymall", null, "2024-01-01", "2024-12-31")).willReturn(List.of());

        mockMvc.perform(get("/benefits")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.benefits").isEmpty());
    }

    @Test
    void 검색_결과가_없으면_빈_목록을_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(benefitService.list(any(), any(), any(), any())).willReturn(List.of());

        mockMvc.perform(get("/benefits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.benefits").isEmpty());
    }

    @Test
    void Cafe24_API_오류는_502로_변환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(benefitService.list(any(), any(), any(), any()))
                .willThrow(new Cafe24ApiException("failed", HttpStatus.INTERNAL_SERVER_ERROR, "{}", null));

        mockMvc.perform(get("/benefits"))
                .andExpect(status().isBadGateway());
    }
}
