package org.example.cafe24_demo_v1.customergroup.presentation;

import org.example.cafe24_demo_v1.customergroup.application.service.CustomerGroupService;
import org.example.cafe24_demo_v1.customergroup.domain.model.CustomerGroup;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerGroupController.class)
class CustomerGroupControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CustomerGroupService customerGroupService;
    @MockitoBean private Cafe24Properties cafe24Properties;

    private CustomerGroup sampleGroup() {
        return new CustomerGroup(1, "일반회원", "기본 등급", "T", "T");
    }

    @Test
    void 회원_등급_목록을_조회한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(customerGroupService.list("mymall")).willReturn(List.of(
                new CustomerGroup(1, "일반회원", "기본 등급", "T", "T"),
                new CustomerGroup(2, "VIP", "VIP 등급", "T", "F")
        ));

        mockMvc.perform(get("/customergroups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.customerGroups[0].groupNo").value(1))
                .andExpect(jsonPath("$.customerGroups[0].groupName").value("일반회원"))
                .andExpect(jsonPath("$.customerGroups[1].groupNo").value(2))
                .andExpect(jsonPath("$.customerGroups[1].groupName").value("VIP"));
    }

    @Test
    void 등급이_없으면_빈_목록을_반환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(customerGroupService.list("mymall")).willReturn(List.of());

        mockMvc.perform(get("/customergroups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.customerGroups").isEmpty());
    }

    @Test
    void Cafe24_API_오류는_502로_변환한다() throws Exception {
        given(cafe24Properties.getMallId()).willReturn("mymall");
        given(customerGroupService.list("mymall"))
                .willThrow(new Cafe24ApiException("failed", HttpStatus.UNAUTHORIZED, "{}", null));

        mockMvc.perform(get("/customergroups"))
                .andExpect(status().isBadGateway());
    }
}
