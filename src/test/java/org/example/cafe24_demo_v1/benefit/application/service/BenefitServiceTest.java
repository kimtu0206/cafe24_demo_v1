package org.example.cafe24_demo_v1.benefit.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.service.Cafe24BenefitPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock private Cafe24BenefitPort cafe24BenefitPort;
    @Mock private AppAuthorizationService authorizationService;

    private BenefitService benefitService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer",
            LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        benefitService = new BenefitService(cafe24BenefitPort, authorizationService);
    }

    @Test
    void 파라미터_없이_조회하면_port의_listBenefits에_위임한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Benefit benefit = new Benefit(1, 3, "T", "Group Sale", "P", "PG", "T", null, null, List.of("P", "M"), "M", List.of(1, 8, 9), "A", "T", null, "T", null, null, null);
        given(cafe24BenefitPort.listBenefits("mymall", null, null, null, credential)).willReturn(List.of(benefit));

        List<Benefit> result = benefitService.list("mymall", null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBenefitName()).isEqualTo("Group Sale");
    }

    @Test
    void useBenefit_T로_조회하면_port에_그대로_전달된다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits("mymall", "T", null, null, credential)).willReturn(List.of());

        List<Benefit> result = benefitService.list("mymall", "T", null, null);

        assertThat(result).isEmpty();
        verify(cafe24BenefitPort).listBenefits("mymall", "T", null, null, credential);
    }

    @Test
    void 기간_파라미터도_port에_그대로_전달된다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits("mymall", null, "2024-01-01", "2024-12-31", credential)).willReturn(List.of());

        benefitService.list("mymall", null, "2024-01-01", "2024-12-31");

        verify(cafe24BenefitPort).listBenefits("mymall", null, "2024-01-01", "2024-12-31", credential);
    }

    @Test
    void getValidCredential을_먼저_호출하고_port에_위임한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits(any(), any(), any(), any(), any())).willReturn(List.of());

        benefitService.list("mymall", "T", null, null);

        InOrder inOrder = Mockito.inOrder(authorizationService, cafe24BenefitPort);
        inOrder.verify(authorizationService).getValidCredential("mymall");
        inOrder.verify(cafe24BenefitPort).listBenefits(any(), any(), any(), any(), any());
    }
}
