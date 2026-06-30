package org.example.cafe24_demo_v1.benefit.domain.repository;

import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;

import java.util.List;
import java.util.Optional;

public interface BenefitRepository {

    void save(Benefit benefit);

    List<Benefit> findByMallId(String mallId, String useBenefit, String startDate, String endDate);

    Optional<Benefit> findByMallIdAndBenefitNo(String mallId, Integer benefitNo);

    void deleteByMallIdAndBenefitNo(String mallId, Integer benefitNo);
}
