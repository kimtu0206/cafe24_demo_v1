package org.example.cafe24_demo_v1.benefit.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BenefitRepositoryAdapter implements BenefitRepository {

    private final BenefitJpaRepository jpaRepository;
    private final BenefitMapper mapper;

    @Override
    public void save(Benefit benefit) {
        BenefitEntity entity = mapper.toEntity(benefit);
        BenefitEntity saved = jpaRepository.save(entity);
        benefit.setId(saved.getId());
    }

    @Override
    public Optional<Benefit> findByMallIdAndBenefitNo(String mallId, Integer benefitNo) {
        return jpaRepository.findByMallIdAndBenefitNo(mallId, benefitNo).map(mapper::toDomain);
    }

    @Override
    public List<Benefit> findByMallId(String mallId, String useBenefit, String startDate, String endDate) {
        String useBenefitParam = StringUtils.hasText(useBenefit) ? useBenefit : null;
        LocalDateTime startParam = StringUtils.hasText(startDate)
                ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime endParam = StringUtils.hasText(endDate)
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

        return jpaRepository.findByMallIdWithFilters(mallId, useBenefitParam, startParam, endParam)
                .stream().map(mapper::toDomain).toList();
    }
}
