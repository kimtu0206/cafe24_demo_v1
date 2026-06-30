package org.example.cafe24_demo_v1.benefit.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.application.command.CreateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.application.command.UpdateBenefitCommand;
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

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void syncFromCafe24는_Cafe24_목록을_조회해_신규항목을_insert한다() {
        Benefit fetched = sampleBenefit();
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits("mymall", null, null, null, credential)).willReturn(List.of(fetched));
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", fetched.getBenefitNo())).willReturn(Optional.empty());

        benefitService.syncFromCafe24("mymall");

        verify(benefitRepository).save(fetched);
        assertThat(fetched.getId()).isNull();
    }

    @Test
    void syncFromCafe24는_기존_항목이_있으면_id를_세팅_후_update한다() {
        Benefit fetched = sampleBenefit();
        Benefit existing = sampleBenefit();
        existing.setId(42L);
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits("mymall", null, null, null, credential)).willReturn(List.of(fetched));
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", fetched.getBenefitNo())).willReturn(Optional.of(existing));

        benefitService.syncFromCafe24("mymall");

        assertThat(fetched.getId()).isEqualTo(42L);
        verify(benefitRepository).save(fetched);
    }

    @Test
    void syncFromCafe24는_Cafe24가_빈_목록을_반환하면_DB를_수정하지_않는다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits("mymall", null, null, null, credential)).willReturn(List.of());

        benefitService.syncFromCafe24("mymall");

        verify(benefitRepository, never()).save(any());
    }

    @Test
    void syncFromCafe24는_여러_혜택을_각각_upsert한다() {
        Benefit b1 = sampleBenefit();
        Benefit b2 = Benefit.register("mymall", 1, 4, "T", "VIP Sale", "P", "PG", "T",
                null, null, List.of("P"), "M", List.of(1), "A", "T", null, "T", null, null, null, null, null);
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits("mymall", null, null, null, credential)).willReturn(List.of(b1, b2));
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", b1.getBenefitNo())).willReturn(Optional.empty());
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", b2.getBenefitNo())).willReturn(Optional.empty());

        benefitService.syncFromCafe24("mymall");

        verify(benefitRepository, times(2)).save(any());
    }

    @Test
    void upsertFromWebhook는_기존_항목이_없으면_신규_저장한다() {
        Benefit fetched = sampleBenefit();
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.getBenefit("mymall", fetched.getBenefitNo(), credential)).willReturn(fetched);
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", fetched.getBenefitNo())).willReturn(Optional.empty());

        benefitService.upsertFromWebhook("mymall", fetched.getBenefitNo());

        verify(benefitRepository).save(fetched);
        assertThat(fetched.getId()).isNull();
    }

    @Test
    void upsertFromWebhook는_기존_항목이_있으면_id를_세팅_후_update한다() {
        Benefit fetched = sampleBenefit();
        Benefit existing = sampleBenefit();
        existing.setId(99L);
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.getBenefit("mymall", fetched.getBenefitNo(), credential)).willReturn(fetched);
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", fetched.getBenefitNo())).willReturn(Optional.of(existing));

        benefitService.upsertFromWebhook("mymall", fetched.getBenefitNo());

        assertThat(fetched.getId()).isEqualTo(99L);
        verify(benefitRepository).save(fetched);
    }

    @Test
    void upsert는_동시_삽입_경쟁으로_충돌하면_재조회후_갱신으로_폴백한다() {
        Benefit fetched = sampleBenefit();
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24BenefitPort.listBenefits("mymall", null, null, null, credential)).willReturn(List.of(fetched));

        Benefit concurrentlyInserted = sampleBenefit();
        concurrentlyInserted.setId(77L);
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", fetched.getBenefitNo()))
                .willReturn(Optional.empty(), Optional.of(concurrentlyInserted));
        // 첫 번째 save는 충돌 예외, 두 번째(catch 블록 폴백)는 정상 처리
        willThrow(new DataIntegrityViolationException("duplicate entry"))
                .willDoNothing()
                .given(benefitRepository).save(fetched);

        benefitService.syncFromCafe24("mymall");

        assertThat(fetched.getId()).isEqualTo(77L);
        verify(benefitRepository, times(2)).save(fetched);
    }

    @Test
    void update는_getValidCredential_후_Cafe24_PUT_호출_후_DB에_upsert한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Benefit updated = sampleBenefit();
        given(cafe24BenefitPort.updateBenefit(any(), any(), any())).willReturn(updated);
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", updated.getBenefitNo())).willReturn(Optional.empty());

        UpdateBenefitCommand command = new UpdateBenefitCommand(
                "mymall", 3, 1, "T", "Updated Benefit", "T",
                "2019-01-01T12:00:00+09:00", "2019-01-31T12:00:00+09:00",
                List.of("P", "M"), "M", List.of(8, 9), "P", "T", "T", null, null
        );

        Benefit result = benefitService.update(command);

        assertThat(result.getBenefitNo()).isEqualTo(3);
        InOrder inOrder = Mockito.inOrder(authorizationService, cafe24BenefitPort, benefitRepository);
        inOrder.verify(authorizationService).getValidCredential("mymall");
        inOrder.verify(cafe24BenefitPort).updateBenefit("mymall", command, credential);
        inOrder.verify(benefitRepository).save(updated);
    }

    @Test
    void update는_기존_항목이_있으면_id를_세팅_후_update한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Benefit updated = sampleBenefit();
        Benefit existing = sampleBenefit();
        existing.setId(55L);
        given(cafe24BenefitPort.updateBenefit(any(), any(), any())).willReturn(updated);
        given(benefitRepository.findByMallIdAndBenefitNo("mymall", updated.getBenefitNo())).willReturn(Optional.of(existing));

        UpdateBenefitCommand command = new UpdateBenefitCommand(
                "mymall", 3, 1, "F", "Updated Benefit", "F",
                null, null, List.of(), null, List.of(), "A", "F", "F", null, null
        );

        benefitService.update(command);

        assertThat(updated.getId()).isEqualTo(55L);
        verify(benefitRepository).save(updated);
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
