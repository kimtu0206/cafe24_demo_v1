package org.example.cafe24_demo_v1.benefit.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.benefit.application.command.CreateBenefitCommand;
import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.model.PeriodSale;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Operation(summary = "혜택 생성", description = "Cafe24에 새 혜택을 생성합니다. 할인(D) 또는 증정(G) 중 선택하며, 기간할인 시 period_sale을 포함합니다.")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateBenefitRequest request) {
        CreateBenefitCommand command = request.toCommand(cafe24Properties.getMallId());
        Benefit benefit = benefitService.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(new BenefitCreatedResponse(BenefitResponse.from(benefit)));
    }

    @ExceptionHandler(Cafe24ApiException.class)
    public ResponseEntity<String> handleCafe24ApiException(Cafe24ApiException e) {
        log.error("Cafe24 benefit API error: {}, body={}", e.getMessage(), e.getResponseBody());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Cafe24 API 호출에 실패했습니다: " + e.getMessage() + "\nCafe24 응답: " + e.getResponseBody());
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
            String memberSale,
            LocalDateTime createdDate,
            PeriodSale periodSale
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
                    benefit.getMemberSale(),
                    benefit.getCreatedDate(),
                    benefit.getPeriodSale()
            );
        }
    }

    private record BenefitListResponse(List<BenefitResponse> benefits, int count) {}

    private record BenefitCreatedResponse(BenefitResponse benefit) {}

    private record CreateBenefitRequest(
            @Schema(example = "1") Integer shopNo,
            @Schema(example = "T", description = "혜택 사용 여부 (T: 사용, F: 미사용)") String useBenefit,
            @Schema(example = "Sample Benefit") String benefitName,
            @Schema(example = "D", description = "혜택 구분 (D: 할인, G: 증정)") String benefitDivision,
            @Schema(example = "DP", description = "혜택 종류 (DP: 기간할인, PG: 구매금액할인 등)") String benefitType,
            @Schema(example = "T", description = "혜택 기간 사용 여부 (T: 사용, F: 미사용)") String useBenefitPeriod,
            @Schema(example = "2019-01-01T12:00:00+09:00") String benefitStartDate,
            @Schema(example = "2019-01-31T12:00:00+09:00") String benefitEndDate,
            @Schema(example = "[\"P\", \"M\"]", description = "적용 플랫폼 (P: PC, M: 모바일)") List<String> platformTypes,
            @Schema(description = "회원 등급 적용 방식 (M: 특정등급, A: 전체등급) — 생략 시 전체 회원 대상") String useGroupBinding,
            @Schema(description = "적용 회원 등급 번호 목록 — 실제 쇼핑몰 등급 번호 사용 (useGroupBinding 지정 시 필수)") List<Integer> customerGroupList,
            @Schema(example = "A", description = "상품 적용 방식 (A: 전체상품, P: 특정상품)") String productBindingType,
            @Schema(example = "F", description = "특정 카테고리 제외 여부 (T: 제외, F: 미제외)") String useExceptCategory,
            @Schema(example = "T", description = "쿠폰 중복 사용 허용 여부 (T: 허용, F: 미허용)") String availableCoupon,
            @Schema(description = "혜택 아이콘 이미지 URL 또는 Base64 인코딩 이미지 — 생략 가능") String iconUrl,
            PeriodSaleRequest periodSale
    ) {
        CreateBenefitCommand toCommand(String mallId) {
            CreateBenefitCommand.PeriodSaleCommand ps = periodSale == null ? null :
                    new CreateBenefitCommand.PeriodSaleCommand(
                            periodSale.productList(),
                            periodSale.exceptCategoryList(),
                            periodSale.discountValue(),
                            periodSale.discountValueUnit(),
                            periodSale.discountTruncationUnit(),
                            periodSale.discountTruncationMethod()
                    );
            return new CreateBenefitCommand(
                    mallId, shopNo, useBenefit, benefitName, benefitDivision, benefitType,
                    useBenefitPeriod, benefitStartDate, benefitEndDate, platformTypes,
                    useGroupBinding, customerGroupList, productBindingType, useExceptCategory,
                    availableCoupon, iconUrl, ps
            );
        }
    }

    private record PeriodSaleRequest(
            @Schema(example = "[]", description = "특정 상품 지정 (productBindingType=P 일 때만 사용)") List<Integer> productList,
            @Schema(example = "[]", description = "제외 카테고리 (useExceptCategory=T 일 때만 사용)") List<Integer> exceptCategoryList,
            @Schema(example = "10.00") String discountValue,
            @Schema(example = "P", description = "할인 단위 (P: 퍼센트, W: 금액)") String discountValueUnit,
            @Schema(example = "O", description = "절사 단위 (O: 원단위, T: 10원단위, H: 100원단위)") String discountTruncationUnit,
            @Schema(example = "U", description = "절사 방법 (U: 올림, D: 내림, R: 반올림)") String discountTruncationMethod
    ) {}
}
