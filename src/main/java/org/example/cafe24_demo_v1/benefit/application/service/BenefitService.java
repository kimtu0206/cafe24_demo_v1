package org.example.cafe24_demo_v1.benefit.application.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.service.Cafe24BenefitPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BenefitService {

    private final Cafe24BenefitPort cafe24BenefitPort;
    private final AppAuthorizationService authorizationService;

    public List<Benefit> list(String mallId, String useBenefit, String startDate, String endDate) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);
        return cafe24BenefitPort.listBenefits(mallId, useBenefit, startDate, endDate, credential);
    }
}
