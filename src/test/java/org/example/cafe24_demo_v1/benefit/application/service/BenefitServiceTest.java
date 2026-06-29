package org.example.cafe24_demo_v1.benefit.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.application.command.CreateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock private Cafe24BenefitPort cafe24BenefitPort;
    @Mock private BenefitRepository benefitRepository;
    @Mock private AppAuthorizationService authorizationService;

    private BenefitService benefitService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer",
            LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        benefitService = new BenefitService(cafe24BenefitPort, benefitRepository, authorizationService);
    }

    private Benefit sampleBenefit() {
        return Benefit.register("mymall", 1, 3, "T", "Group Sale", "P", "PG", "T",
                null, null, List.of("P", "M"), "M", List.of(1, 8, 9),
                "A", "T", null, "T", null, null, null, null, null);
    }

    @Test
    void list는_로컬DB에서_조회하며_Cafe24를_호출하지_않는다() {
        given(benefitRepository.findByMallId("mymall", null, null, null)).willReturn(List.of(sampleBenefit()));

        List<Benefit> result = benefitService.list("mymall", null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBenefitName()).isEqualTo("Group Sale");
        verify(benefitRepository).findByMallId("mymall", null, null, null);
        verifyNoInteractions(cafe24BenefitPort);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void list는_useBenefit_파라미터를_repository에_그대로_전달한다() {
        given(benefitRepository.findByMallId("mymall", "T", null, null)).willReturn(List.of());

        benefitService.list("mymall", "T", null, null);

        verify(benefitRepository).findByMallId("mymall", "T", null, null);
    }

    @Test
    void list는_기간_파라미터를_repository에_그대로_전달한다() {
        given(benefitRepository.findByMallId("mymall", null, "2024-01-01", "2024-12-31")).willReturn(List.of());

        benefitService.list("mymall", null, "2024-01-01", "2024-12-31");

        verify(benefitRepository).findByMallId("mymall", null, "2024-01-01", "2024-12-31");
    }

    @Test
    void create는_getValidCredential_후_Cafe24_API_호출_후_DB에_저장한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Benefit created = sampleBenefit();
        given(cafe24BenefitPort.createBenefit(any(), any(), any())).willReturn(created);

        CreateBenefitCommand command = new CreateBenefitCommand(
                "mymall", 1, "T", "Sample Benefit", "D", "DP", "T",
                "2019-01-01T12:00:00+09:00", "2019-01-31T12:00:00+09:00",
                List.of("P", "M"), "M", List.of(8, 9), "P", "T", "T", null, null
        );

        Benefit result = benefitService.create(command);

        assertThat(result.getBenefitNo()).isEqualTo(3);
        InOrder inOrder = Mockito.inOrder(authorizationService, cafe24BenefitPort, benefitRepository);
        inOrder.verify(authorizationService).getValidCredential("mymall");
        inOrder.verify(cafe24BenefitPort).createBenefit("mymall", command, credential);
        inOrder.verify(benefitRepository).save(created);
    }

    @Test
    void create는_Cafe24_API가_반환한_Benefit을_그대로_반환한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Benefit expected = sampleBenefit();
        given(cafe24BenefitPort.createBenefit(any(), any(), any())).willReturn(expected);

        CreateBenefitCommand command = new CreateBenefitCommand(
                "mymall", 1, "T", "Group Sale", "D", "DP", "F",
                null, null, List.of(), "N", List.of(), "A", "F", "F", null, null
        );

        Benefit result = benefitService.create(command);

        assertThat(result).isSameAs(expected);
    }
}
