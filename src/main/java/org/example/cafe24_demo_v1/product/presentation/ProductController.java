package org.example.cafe24_demo_v1.product.presentation;

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
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final Cafe24Properties cafe24Properties;

    @PostMapping
    public ResponseEntity<ProductResponse> register(@Valid @RequestBody ProductRegisterRequest request) {
        Product product = productService.register(
                new CreateProductCommand(cafe24Properties.getMallId(), request.productName(), request.price(), request.supplyPrice())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    @PutMapping("/{productNo}")
    public ResponseEntity<ProductResponse> update(@PathVariable Long productNo, @Valid @RequestBody ProductUpdateRequest request) {
        Product product = productService.update(
                new UpdateProductCommand(cafe24Properties.getMallId(), productNo, request.productName(), request.price(), request.supplyPrice())
        );
        return ResponseEntity.ok(ProductResponse.from(product));
    }

    @DeleteMapping("/{productNo}")
    public ResponseEntity<Void> delete(@PathVariable Long productNo) {
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
            @NotBlank String productName,
            @NotNull @PositiveOrZero BigDecimal price,
            @NotNull @PositiveOrZero BigDecimal supplyPrice
    ) {}

    private record ProductUpdateRequest(
            @NotBlank String productName,
            @NotNull @PositiveOrZero BigDecimal price,
            @NotNull @PositiveOrZero BigDecimal supplyPrice
    ) {}

    private record ProductResponse(Long productNo, String productName, BigDecimal price, String status) {
        static ProductResponse from(Product product) {
            return new ProductResponse(product.getProductNo(), product.getProductName(), product.getPrice(), product.getStatus().name());
        }
    }
}
