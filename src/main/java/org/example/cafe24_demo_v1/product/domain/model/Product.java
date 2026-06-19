package org.example.cafe24_demo_v1.product.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Cafe24에 등록된 상품을 나타내는 도메인 모델.
 *
 * 상품의 원천 데이터는 항상 Cafe24이며, 이 모델은 Cafe24 응답을 로컬 DB에 보관하기 위한
 * 표현이다. 외부에서 필드를 직접 수정하지 못하도록 setter를 열지 않는다.
 */
@Getter
public class Product {

    private Long id;                  // DB PK (영속화 후 채워짐)
    private Long productNo;           // Cafe24 상품 번호
    private String mallId;            // 상품이 등록된 쇼핑몰 ID
    private String productName;
    private BigDecimal price;
    private BigDecimal supplyPrice;
    private ProductStatus status;
    private String description;
    private String paymentInfo;
    private String shippingInfo;
    private String exchangeInfo;
    private BigDecimal priceExcludingTax;
    private String detailImage;
    private String imageUploadType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Product() {}

    /** Cafe24 상품 등록/조회 응답을 받아 신규 로컬 상품을 생성할 때 사용한다. */
    public static Product register(
            String mallId,
            Long productNo,
            String productName,
            BigDecimal price,
            BigDecimal supplyPrice,
            ProductStatus status,
            String description,
            String paymentInfo,
            String shippingInfo,
            String exchangeInfo,
            BigDecimal priceExcludingTax,
            String detailImage,
            String imageUploadType
    ) {
        Product product = new Product();
        product.mallId = mallId;
        product.productNo = productNo;
        product.productName = productName;
        product.price = price;
        product.supplyPrice = supplyPrice;
        product.status = status;
        product.description = description;
        product.paymentInfo = paymentInfo;
        product.shippingInfo = shippingInfo;
        product.exchangeInfo = exchangeInfo;
        product.priceExcludingTax = priceExcludingTax;
        product.detailImage = detailImage;
        product.imageUploadType = imageUploadType;
        product.createdAt = LocalDateTime.now();
        product.updatedAt = LocalDateTime.now();
        return product;
    }

    /** DB에서 조회한 데이터로 도메인 객체를 복원할 때 사용한다. */
    public static Product reconstitute(
            Long id,
            Long productNo,
            String mallId,
            String productName,
            BigDecimal price,
            BigDecimal supplyPrice,
            ProductStatus status,
            String description,
            String paymentInfo,
            String shippingInfo,
            String exchangeInfo,
            BigDecimal priceExcludingTax,
            String detailImage,
            String imageUploadType,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Product product = new Product();
        product.id = id;
        product.productNo = productNo;
        product.mallId = mallId;
        product.productName = productName;
        product.price = price;
        product.supplyPrice = supplyPrice;
        product.status = status;
        product.description = description;
        product.paymentInfo = paymentInfo;
        product.shippingInfo = shippingInfo;
        product.exchangeInfo = exchangeInfo;
        product.priceExcludingTax = priceExcludingTax;
        product.detailImage = detailImage;
        product.imageUploadType = imageUploadType;
        product.createdAt = createdAt;
        product.updatedAt = updatedAt;
        return product;
    }

    /**
     * Cafe24로부터 받은 최신 정보로 상품 정보를 갱신한다.
     * 스케줄러 동기화, Webhook 상품 생성 알림 처리 시 사용한다.
     */
    public void applySnapshot(
            String productName,
            BigDecimal price,
            BigDecimal supplyPrice,
            ProductStatus status,
            String description,
            String paymentInfo,
            String shippingInfo,
            String exchangeInfo,
            BigDecimal priceExcludingTax,
            String detailImage,
            String imageUploadType
    ) {
        Objects.requireNonNull(productName);
        Objects.requireNonNull(status);
        this.productName = productName;
        this.price = price;
        this.supplyPrice = supplyPrice;
        this.status = status;
        this.description = description;
        this.paymentInfo = paymentInfo;
        this.shippingInfo = shippingInfo;
        this.exchangeInfo = exchangeInfo;
        this.priceExcludingTax = priceExcludingTax;
        this.detailImage = detailImage;
        this.imageUploadType = imageUploadType;
        this.updatedAt = LocalDateTime.now();
    }

    // 패키지 내부에서만 호출 가능 — DB 저장 후 생성된 PK를 주입할 때 사용
    public void setId(Long id) { this.id = id; }
}
