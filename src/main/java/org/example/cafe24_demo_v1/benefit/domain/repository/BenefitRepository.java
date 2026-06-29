package org.example.cafe24_demo_v1.benefit.domain.repository;

import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;

import java.util.List;

public interface BenefitRepository {

    void save(Benefit benefit);

    List<Benefit> findByMallId(String mallId, String useBenefit, String startDate, String endDate);
}
