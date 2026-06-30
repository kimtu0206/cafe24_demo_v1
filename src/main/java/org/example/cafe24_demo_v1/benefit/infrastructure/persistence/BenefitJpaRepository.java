package org.example.cafe24_demo_v1.benefit.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

interface BenefitJpaRepository extends JpaRepository<BenefitEntity, Long> {

    @Query("SELECT b FROM BenefitEntity b WHERE b.mallId = :mallId " +
           "AND (:useBenefit IS NULL OR b.useBenefit = :useBenefit) " +
           "AND (:startDate IS NULL OR b.benefitStartDate >= :startDate) " +
           "AND (:endDate IS NULL OR b.benefitStartDate <= :endDate) " +
           "ORDER BY b.createdAt DESC")
    List<BenefitEntity> findByMallIdWithFilters(
            @Param("mallId") String mallId,
            @Param("useBenefit") String useBenefit,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    Optional<BenefitEntity> findByMallIdAndBenefitNo(String mallId, Integer benefitNo);

    void deleteByMallIdAndBenefitNo(String mallId, Integer benefitNo);
}
