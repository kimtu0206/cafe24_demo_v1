package org.example.cafe24_demo_v1.benefit.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.model.PeriodSale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
class BenefitMapper {

    private final ObjectMapper objectMapper;

    Benefit toDomain(BenefitEntity entity) {
        return Benefit.reconstitute(
                entity.getId(),
                entity.getMallId(),
                entity.getShopNo(),
                entity.getBenefitNo(),
                entity.getUseBenefit(),
                entity.getBenefitName(),
                entity.getBenefitDivision(),
                entity.getBenefitType(),
                entity.getUseBenefitPeriod(),
                entity.getBenefitStartDate(),
                entity.getBenefitEndDate(),
                parseStringList(entity.getPlatformTypes()),
                entity.getUseGroupBinding(),
                parseIntegerList(entity.getCustomerGroupList()),
                entity.getProductBindingType(),
                entity.getUseExceptCategory(),
                entity.getIconUrl(),
                entity.getAvailableCoupon(),
                entity.getRepurchaseSale(),
                entity.getBulkPurchaseSale(),
                entity.getMemberSale(),
                entity.getCafe24CreatedDate(),
                parsePeriodSale(entity.getPeriodSale()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    BenefitEntity toEntity(Benefit benefit) {
        BenefitEntity entity = new BenefitEntity();
        entity.setId(benefit.getId());
        entity.setMallId(benefit.getMallId());
        entity.setShopNo(benefit.getShopNo());
        entity.setBenefitNo(benefit.getBenefitNo());
        entity.setUseBenefit(benefit.getUseBenefit());
        entity.setBenefitName(benefit.getBenefitName());
        entity.setBenefitDivision(benefit.getBenefitDivision());
        entity.setBenefitType(benefit.getBenefitType());
        entity.setUseBenefitPeriod(benefit.getUseBenefitPeriod());
        entity.setBenefitStartDate(benefit.getBenefitStartDate());
        entity.setBenefitEndDate(benefit.getBenefitEndDate());
        entity.setPlatformTypes(joinList(benefit.getPlatformTypes()));
        entity.setUseGroupBinding(benefit.getUseGroupBinding());
        entity.setCustomerGroupList(joinIntegerList(benefit.getCustomerGroupList()));
        entity.setProductBindingType(benefit.getProductBindingType());
        entity.setUseExceptCategory(benefit.getUseExceptCategory());
        entity.setIconUrl(benefit.getIconUrl());
        entity.setAvailableCoupon(benefit.getAvailableCoupon());
        entity.setRepurchaseSale(benefit.getRepurchaseSale());
        entity.setBulkPurchaseSale(benefit.getBulkPurchaseSale());
        entity.setMemberSale(benefit.getMemberSale());
        entity.setCafe24CreatedDate(benefit.getCreatedDate());
        entity.setPeriodSale(serializePeriodSale(benefit.getPeriodSale()));
        entity.setCreatedAt(benefit.getCreatedAt());
        entity.setUpdatedAt(benefit.getUpdatedAt());
        return entity;
    }

    private List<String> parseStringList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.asList(value.split(","));
    }

    private List<Integer> parseIntegerList(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }

    private String joinList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        return String.join(",", list);
    }

    private String joinIntegerList(List<Integer> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private PeriodSale parsePeriodSale(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return objectMapper.readValue(json, PeriodSale.class);
        } catch (JsonProcessingException e) {
            log.warn("period_sale JSON 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    private String serializePeriodSale(PeriodSale periodSale) {
        if (periodSale == null) return null;
        try {
            return objectMapper.writeValueAsString(periodSale);
        } catch (JsonProcessingException e) {
            log.warn("period_sale JSON 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
