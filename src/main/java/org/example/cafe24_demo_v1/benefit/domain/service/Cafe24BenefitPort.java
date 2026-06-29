package org.example.cafe24_demo_v1.benefit.domain.service;

import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;

import java.util.List;

public interface Cafe24BenefitPort {

    List<Benefit> listBenefits(String mallId, String useBenefit, String startDate, String endDate, TokenCredential credential);
}
