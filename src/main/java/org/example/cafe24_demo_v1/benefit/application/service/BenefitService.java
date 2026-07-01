package org.example.cafe24_demo_v1.benefit.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.application.command.CreateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.application.command.UpdateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitRepository;
import org.example.cafe24_demo_v1.benefit.domain.service.Cafe24BenefitPort;
import org.example.cafe24_demo_v1.shared.application.ConcurrentUpsert;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BenefitService {

    private final Cafe24BenefitPort cafe24BenefitPort;
    private final BenefitRepository benefitRepository;
    private final AppAuthorizationService authorizationService;

    /** 로컬 DB에 저장된 혜택 목록을 조회한다. Cafe24를 호출하지 않는다. */
    public List<Benefit> list(String mallId, String useBenefit, String startDate, String endDate) {
        return benefitRepository.findByMallId(mallId, useBenefit, startDate, endDate);
    }

    /** Cafe24에 혜택을 생성하고 결과를 로컬 DB에 저장한다. */
    public Benefit create(CreateBenefitCommand command) {
        TokenCredential credential = authorizationService.getValidCredential(command.mallId());
        Benefit benefit = cafe24BenefitPort.createBenefit(command.mallId(), command, credential);
        benefitRepository.save(benefit);
        return benefit;
    }

    /** Cafe24 혜택을 삭제하고 로컬 DB에서도 제거한다. */
    @Transactional
    public void delete(String mallId, Integer benefitNo) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        cafe24BenefitPort.deleteBenefit(mallId, benefitNo, credential);
        benefitRepository.deleteByMallIdAndBenefitNo(mallId, benefitNo);
    }

    /** Webhook으로 혜택 삭제 알림을 받았을 때 호출한다. Cafe24에 이미 삭제된 상태이므로 로컬 DB에서만 제거한다. */
    @Transactional
    public void deleteFromWebhook(String mallId, Integer benefitNo) {
        benefitRepository.deleteByMallIdAndBenefitNo(mallId, benefitNo);
    }

    /** Cafe24 혜택을 수정하고 결과를 로컬 DB에 반영한다(Upsert). */
    public Benefit update(UpdateBenefitCommand command) {
        TokenCredential credential = authorizationService.getValidCredential(command.mallId());
        Benefit benefit = cafe24BenefitPort.updateBenefit(command.mallId(), command, credential);
        upsert(benefit);
        return benefit;
    }

    /** Cafe24 전체 혜택 목록을 조회해 로컬 DB와 동기화한다(Upsert). 스케줄러가 주기적으로 호출한다. */
    public void syncFromCafe24(String mallId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        List<Benefit> benefits = cafe24BenefitPort.listBenefits(mallId, null, null, null, credential);
        log.info("Benefit sync: mallId={}, fetched={}", mallId, benefits.size());
        for (Benefit fetched : benefits) {
            upsert(fetched);
        }
    }

    /** Webhook으로 혜택 등록 알림을 받았을 때 호출한다. Cafe24에서 상세 정보를 재조회해 로컬 DB에 반영한다(Upsert). */
    public void upsertFromWebhook(String mallId, Integer benefitNo) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        Benefit fetched = cafe24BenefitPort.getBenefit(mallId, benefitNo, credential);
        upsert(fetched);
    }

    private void upsert(Benefit snapshot) {
        ConcurrentUpsert.apply(
                "Benefit",
                snapshot.getMallId() + "/" + snapshot.getBenefitNo(),
                () -> benefitRepository.findByMallIdAndBenefitNo(snapshot.getMallId(), snapshot.getBenefitNo()),
                existing -> {
                    snapshot.setId(existing.getId());
                    benefitRepository.save(snapshot);
                },
                () -> benefitRepository.save(snapshot)
        );
    }
}
