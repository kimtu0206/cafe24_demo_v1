package org.example.cafe24_demo_v1.benefit.presentation;

import org.example.cafe24_demo_v1.benefit.application.command.CreateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.application.command.UpdateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.model.PeriodSale;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BenefitController.class)
class BenefitControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BenefitService benefitService;
    @MockitoBean private Cafe24Properties cafe24Properties;

    private Benefit sampleBenefit() {
        return Benefit.register("mymall", 1, 3, "T", "Group Sale", "P", "PG", "T",
                null, null, List.of("P", "M"), "M", List.of(1, 8, 9),
                "A", "T", null, "T", null, null, null, null, null);
    }

    @Test
    void 파라미터_없이_전체_혜택을_조회한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(benefitService.list("mymall", null, null, null)).willReturn(List.of(sampleBenefit()));

        mockMvc.perform(get("/benefits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.benefits[0].benefitNo").value(3))
                .andExpect(jsonPath("$.benefits[0].benefitName").value("Group Sale"));
    }

    @Test
    void useBenefit_T로_진행중_혜택만_조회한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Benefit benefit = Benefit.register("mymall", 1, 3, "T", "Group Sale", "P", "PG", "T",
                null, null, List.of(), "N", List.of(), "A", "T", null, "T",
                null, null, null, null, null);
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

    @Test
    void 혜택_생성_요청이_성공하면_201과_생성된_혜택을_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        PeriodSale periodSale = new PeriodSale(List.of(17, 25), null, List.of(168), null, "10.00", "P", "O", "U");
        Benefit created = Benefit.register("mymall", 1, 3, "T", "Sample Benefit", "D", "DP", "T",
                null, null, List.of("P", "M"), "M", List.of(8, 9),
                "P", "T", "https://example.com/icon.png", "T", null, null, null, null, periodSale);
        given(benefitService.create(any(CreateBenefitCommand.class))).willReturn(created);

        mockMvc.perform(post("/benefits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopNo": 1,
                                  "useBenefit": "T",
                                  "benefitName": "Sample Benefit",
                                  "benefitDivision": "D",
                                  "benefitType": "DP",
                                  "useBenefitPeriod": "T",
                                  "benefitStartDate": "2019-01-01T12:00:00+09:00",
                                  "benefitEndDate": "2019-01-31T12:00:00+09:00",
                                  "platformTypes": ["P", "M"],
                                  "useGroupBinding": "M",
                                  "customerGroupList": [8, 9],
                                  "productBindingType": "P",
                                  "useExceptCategory": "T",
                                  "availableCoupon": "T",
                                  "periodSale": {
                                    "productList": [17, 25],
                                    "exceptCategoryList": [168],
                                    "discountValue": "10.00",
                                    "discountValueUnit": "P",
                                    "discountTruncationUnit": "O",
                                    "discountTruncationMethod": "U"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.benefit.benefitNo").value(3))
                .andExpect(jsonPath("$.benefit.benefitName").value("Sample Benefit"))
                .andExpect(jsonPath("$.benefit.periodSale.discountValue").value("10.00"));
    }

    @Test
    void 혜택_생성_중_Cafe24_오류는_502로_변환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(benefitService.create(any(CreateBenefitCommand.class)))
                .willThrow(new Cafe24ApiException("failed", HttpStatus.BAD_REQUEST, "{}", null));

        mockMvc.perform(post("/benefits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopNo": 1,
                                  "useBenefit": "T",
                                  "benefitName": "Test",
                                  "benefitDivision": "D",
                                  "benefitType": "DP"
                                }
                                """))
                .andExpect(status().isBadGateway());
    }

    @Test
    void 혜택_수정_요청이_성공하면_200과_수정된_혜택을_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        Benefit updated = Benefit.register("mymall", 1, 3, "T", "Updated Benefit", "D", "DP", "T",
                null, null, List.of("P", "M"), "M", List.of(8, 9),
                "P", "T", null, "T", null, null, null, null, null);
        given(benefitService.update(any(UpdateBenefitCommand.class))).willReturn(updated);

        mockMvc.perform(put("/benefits/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopNo": 1,
                                  "useBenefit": "T",
                                  "benefitName": "Updated Benefit",
                                  "useBenefitPeriod": "T",
                                  "benefitStartDate": "2019-01-01T12:00:00+09:00",
                                  "benefitEndDate": "2019-01-31T12:00:00+09:00",
                                  "platformTypes": ["P", "M"],
                                  "useGroupBinding": "M",
                                  "customerGroupList": [8, 9],
                                  "productBindingType": "P",
                                  "useExceptCategory": "T",
                                  "availableCoupon": "T"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.benefit.benefitNo").value(3))
                .andExpect(jsonPath("$.benefit.benefitName").value("Updated Benefit"));
    }

    @Test
    void 혜택_수정_중_Cafe24_오류는_502로_변환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(benefitService.update(any(UpdateBenefitCommand.class)))
                .willThrow(new Cafe24ApiException("failed", HttpStatus.BAD_REQUEST, "{}", null));

        mockMvc.perform(put("/benefits/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shopNo": 1,
                                  "useBenefit": "T",
                                  "benefitName": "Updated Benefit"
                                }
                                """))
                .andExpect(status().isBadGateway());
    }

    @Test
    void 혜택_삭제_요청이_성공하면_200과_benefitNo를_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");

        mockMvc.perform(delete("/benefits/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.benefit.benefitNo").value(3));

        verify(benefitService).delete("mymall", 3);
    }

    @Test
    void 혜택_삭제_중_Cafe24_오류는_502로_변환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        willThrow(new Cafe24ApiException("failed", HttpStatus.BAD_REQUEST, "{}", null))
                .given(benefitService).delete("mymall", 3);

        mockMvc.perform(delete("/benefits/3"))
                .andExpect(status().isBadGateway());
    }
}
