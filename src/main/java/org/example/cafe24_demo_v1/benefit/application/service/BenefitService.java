package org.example.cafe24_demo_v1.benefit.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.application.command.CreateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitRepository;
import org.example.cafe24_demo_v1.benefit.domain.service.Cafe24BenefitPort;
import org.springframework.stereotype.Service;

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

    /** Cafe24 전체 혜택 목록을 조회해 로컬 DB와 동기화한다(Upsert). 스케줄러가 주기적으로 호출한다. */
    public void syncFromCafe24(String mallId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        List<Benefit> benefits = cafe24BenefitPort.listBenefits(mallId, null, null, null, credential);
        log.info("Benefit sync: mallId={}, fetched={}", mallId, benefits.size());
        for (Benefit fetched : benefits) {
            benefitRepository.findByMallIdAndBenefitNo(mallId, fetched.getBenefitNo())
                    .ifPresentOrElse(
                            existing -> {
                                fetched.setId(existing.getId());
                                benefitRepository.save(fetched);
                            },
                            () -> benefitRepository.save(fetched)
                    );
        }
    }

    /** Webhook으로 혜택 등록 알림을 받았을 때 호출한다. Cafe24에서 상세 정보를 재조회해 로컬 DB에 반영한다(Upsert). */
    public void upsertFromWebhook(String mallId, Integer benefitNo) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        Benefit fetched = cafe24BenefitPort.getBenefit(mallId, benefitNo, credential);
        benefitRepository.findByMallIdAndBenefitNo(mallId, benefitNo)
                .ifPresentOrElse(
                        existing -> {
                            fetched.setId(existing.getId());
                            benefitRepository.save(fetched);
                        },
                        () -> benefitRepository.save(fetched)
                );
    }
}
