package org.example.cafe24_demo_v1.benefit.application.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.application.command.CreateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitRepository;
import org.example.cafe24_demo_v1.benefit.domain.service.Cafe24BenefitPort;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
