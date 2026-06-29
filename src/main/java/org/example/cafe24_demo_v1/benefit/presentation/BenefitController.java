package org.example.cafe24_demo_v1.benefit.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Benefits", description = "혜택 목록 조회 — Cafe24 Admin API 연동 (실패 시 502 반환)")
@Slf4j
@RestController
@RequestMapping("/benefits")
@RequiredArgsConstructor
public class BenefitController {

    private final BenefitService benefitService;
    private final Cafe24Properties cafe24Properties;

    @Operation(
            summary = "혜택 목록 조회",
            description = "진행 여부, 기간으로 Cafe24 혜택 목록을 조회합니다. 파라미터를 모두 생략하면 전체 혜택을 반환합니다."
    )
    @GetMapping
    public ResponseEntity<?> list(
            @Parameter(description = "혜택 사용 여부 (T: 진행중, F: 미사용)") @RequestParam(required = false) String useBenefit,
            @Parameter(description = "혜택 시작일 범위 시작 (예: 2024-01-01)") @RequestParam(required = false) String startDate,
            @Parameter(description = "혜택 시작일 범위 종료 (예: 2024-12-31)") @RequestParam(required = false) String endDate
    ) {
        List<Benefit> benefits = benefitService.list(cafe24Properties.getMallId(), useBenefit, startDate, endDate);
        List<BenefitResponse> response = benefits.stream().map(BenefitResponse::from).toList();
        return ResponseEntity.ok(new BenefitListResponse(response, response.size()));
    }

    @ExceptionHandler(Cafe24ApiException.class)
    public ResponseEntity<String> handleCafe24ApiException(Cafe24ApiException e) {
        log.error("Cafe24 benefit API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Cafe24 API 호출에 실패했습니다: " + e.getMessage());
    }

    private record BenefitResponse(
            Integer shopNo,
            Integer benefitNo,
            String useBenefit,
            String benefitName,
            String benefitDivision,
            String benefitType,
            String useBenefitPeriod,
            LocalDateTime benefitStartDate,
            LocalDateTime benefitEndDate,
            List<String> platformTypes,
            String useGroupBinding,
            List<Integer> customerGroupList,
            String productBindingType,
            String useExceptCategory,
            String iconUrl,
            String availableCoupon,
            String repurchaseSale,
            String bulkPurchaseSale,
            String memberSale
    ) {
        static BenefitResponse from(Benefit benefit) {
            return new BenefitResponse(
                    benefit.getShopNo(),
                    benefit.getBenefitNo(),
                    benefit.getUseBenefit(),
                    benefit.getBenefitName(),
                    benefit.getBenefitDivision(),
                    benefit.getBenefitType(),
                    benefit.getUseBenefitPeriod(),
                    benefit.getBenefitStartDate(),
                    benefit.getBenefitEndDate(),
                    benefit.getPlatformTypes(),
                    benefit.getUseGroupBinding(),
                    benefit.getCustomerGroupList(),
                    benefit.getProductBindingType(),
                    benefit.getUseExceptCategory(),
                    benefit.getIconUrl(),
                    benefit.getAvailableCoupon(),
                    benefit.getRepurchaseSale(),
                    benefit.getBulkPurchaseSale(),
                    benefit.getMemberSale()
            );
        }
    }

    private record BenefitListResponse(List<BenefitResponse> benefits, int count) {}
}
