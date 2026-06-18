package org.example.cafe24_demo_v1.product.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.product.application.command.CreateProductCommand;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.product.domain.model.Product;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ResponseEntity<ProductResponse> register(@RequestBody ProductRegisterRequest request) {
        Product product = productService.register(
                new CreateProductCommand(cafe24Properties.getMallId(), request.productName(), request.price(), request.supplyPrice())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(product));
    }

    /** Cafe24 API 호출 실패는 우리 책임이 아니므로 502(Bad Gateway)로 응답한다. */
    @ExceptionHandler(Cafe24ApiException.class)
    public ResponseEntity<String> handleCafe24ApiException(Cafe24ApiException e) {
        log.error("Cafe24 product API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Cafe24 API 호출에 실패했습니다: " + e.getMessage());
    }

    private record ProductRegisterRequest(String productName, BigDecimal price, BigDecimal supplyPrice) {}

    private record ProductResponse(Long productNo, String productName, BigDecimal price, String status) {
        static ProductResponse from(Product product) {
            return new ProductResponse(product.getProductNo(), product.getProductName(), product.getPrice(), product.getStatus().name());
        }
    }
}
