package org.example.cafe24_demo_v1.product.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.product.application.command.CreateProductCommand;
import org.example.cafe24_demo_v1.product.application.command.DeleteProductCommand;
import org.example.cafe24_demo_v1.product.application.command.UpdateProductCommand;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.product.domain.model.ProductPage;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Products", description = "상품 CRUD — Cafe24 API 연동 (실패 시 502 반환)")
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final Cafe24Properties cafe24Properties;

    @Operation(summary = "상품 목록 조회", description = "로컬 DB에 저장된 상품 목록을 페이지 단위로 조회합니다.")
    @GetMapping
    public ResponseEntity<ProductListResponse> list(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size
    ) {
        ProductPage result = productService.list(cafe24Properties.getMallId(), page, size);
        List<ProductResponse> products = result.products().stream().map(ProductResponse::from).toList();
        return ResponseEntity.ok(new ProductListResponse(products, result.totalCount(), page, size));
    }

    @Operation(summary = "상품 등록", description = "Cafe24에 상품을 등록하고 로컬 DB에 저장합니다.")
    @PostMapping
    public ResponseEntity<ProductResponse> register(@Valid @RequestBody ProductRegisterRequest request) {
        Product product = productService.register(
                new CreateProductCommand(
                        cafe24Properties.getMallId(), request.productName(), request.price(), request.supplyPrice(),
                        request.description(), request.paymentInfo(), request.shippingInfo(), request.exchangeInfo(),
                        request.priceExcludingTax(), request.detailImage(), request.imageUploadType()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @Operation(summary = "상품 수정", description = "Cafe24 상품을 수정하고 로컬 DB를 갱신합니다.")
    @PutMapping("/{productNo}")
    public ResponseEntity<ProductResponse> update(
            @Parameter(description = "수정할 상품 번호") @PathVariable Long productNo,
            @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productService.update(
                new UpdateProductCommand(
                        cafe24Properties.getMallId(), productNo, request.productName(), request.price(), request.supplyPrice(),
                        request.description(), request.paymentInfo(), request.shippingInfo(), request.exchangeInfo(),
                        request.priceExcludingTax(), request.detailImage(), request.imageUploadType()
                )
        );
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @Operation(summary = "상품 삭제", description = "Cafe24 상품을 삭제하고 로컬 DB에서도 제거합니다.")
    @DeleteMapping("/{productNo}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "삭제할 상품 번호") @PathVariable Long productNo) {
        productService.delete(new DeleteProductCommand(cafe24Properties.getMallId(), productNo));
        return ResponseEntity.noContent().build();
    }

    /** Cafe24 API 호출 실패는 우리 책임이 아니므로 502(Bad Gateway)로 응답한다. */
    @ExceptionHandler(Cafe24ApiException.class)
    public ResponseEntity<String> handleCafe24ApiException(Cafe24ApiException e) {
        log.error("Cafe24 product API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Cafe24 API 호출에 실패했습니다: " + e.getMessage());
    }

    private record ProductRegisterRequest(
            @Schema(example = "레더 크로스백") @NotBlank String productName,
            @Schema(example = "89000") @NotNull @PositiveOrZero BigDecimal price,
            @Schema(example = "45000") @NotNull @PositiveOrZero BigDecimal supplyPrice,
            @Schema(example = "고급 레더 소재의 크로스백입니다.") String description,
            @Schema(example = "무통장 입금, 신용카드 결제 가능") String paymentInfo,
            @Schema(example = "CJ대한통운 3~5일 이내 배송") String shippingInfo,
            @Schema(example = "수령 후 7일 이내 교환/반품 가능") String exchangeInfo,
            @Schema(example = "80909") @PositiveOrZero BigDecimal priceExcludingTax,
            @Schema(example = "https://example.com/images/product.jpg") String detailImage,
            @Schema(example = "A") String imageUploadType
    ) {}

    private record ProductUpdateRequest(
            @Schema(example = "레더 크로스백 (개정판)") @NotBlank String productName,
            @Schema(example = "79000") @NotNull @PositiveOrZero BigDecimal price,
            @Schema(example = "40000") @NotNull @PositiveOrZero BigDecimal supplyPrice,
            @Schema(example = "고급 레더 소재의 크로스백입니다. (색상 추가)") String description,
            @Schema(example = "무통장 입금, 신용카드 결제 가능") String paymentInfo,
            @Schema(example = "CJ대한통운 3~5일 이내 배송") String shippingInfo,
            @Schema(example = "수령 후 7일 이내 교환/반품 가능") String exchangeInfo,
            @Schema(example = "71818") @PositiveOrZero BigDecimal priceExcludingTax,
            @Schema(example = "https://example.com/images/product-v2.jpg") String detailImage,
            @Schema(example = "A") String imageUploadType
    ) {}

    private record ProductResponse(
            Long productNo,
            String productName,
            BigDecimal price,
            String status,
            String description,
            String paymentInfo,
            String shippingInfo,
            String exchangeInfo,
            BigDecimal priceExcludingTax,
            String detailImage,
            String imageUploadType
    ) {
        static ProductResponse from(Product product) {
            return new ProductResponse(
                    product.getProductNo(), product.getProductName(), product.getPrice(), product.getStatus().name(),
                    product.getDescription(), product.getPaymentInfo(), product.getShippingInfo(), product.getExchangeInfo(),
                    product.getPriceExcludingTax(), product.getDetailImage(), product.getImageUploadType()
            );
        }
    }

    private record ProductListResponse(List<ProductResponse> products, long totalCount, int page, int size) {}
}
