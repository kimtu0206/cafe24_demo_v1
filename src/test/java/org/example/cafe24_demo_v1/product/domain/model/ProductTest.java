package org.example.cafe24_demo_v1.product.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void register로_생성하면_입력한_값으로_초기화된다() {
        Product product = Product.register(
                "mymall", 100L, "테스트 상품", new BigDecimal("10000"), new BigDecimal("5000"), ProductStatus.ON_SALE,
                "설명", "결제 안내", "배송 안내", "교환 안내", new BigDecimal("9090"), "/detail.jpg", "A"
        );

        assertThat(product.getMallId()).isEqualTo("mymall");
        assertThat(product.getProductNo()).isEqualTo(100L);
        assertThat(product.getProductName()).isEqualTo("테스트 상품");
        assertThat(product.getPrice()).isEqualTo(new BigDecimal("10000"));
        assertThat(product.getSupplyPrice()).isEqualTo(new BigDecimal("5000"));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getDescription()).isEqualTo("설명");
        assertThat(product.getPaymentInfo()).isEqualTo("결제 안내");
        assertThat(product.getShippingInfo()).isEqualTo("배송 안내");
        assertThat(product.getExchangeInfo()).isEqualTo("교환 안내");
        assertThat(product.getPriceExcludingTax()).isEqualTo(new BigDecimal("9090"));
        assertThat(product.getDetailImage()).isEqualTo("/detail.jpg");
        assertThat(product.getImageUploadType()).isEqualTo("A");
        assertThat(product.getCreatedAt()).isNotNull();
    }

    @Test
    void applySnapshot으로_상품_정보를_갱신할_수_있다() {
        Product product = Product.register(
                "mymall", 100L, "기존 상품명", new BigDecimal("10000"), new BigDecimal("5000"), ProductStatus.ON_SALE,
                "기존 설명", "기존 결제 안내", "기존 배송 안내", "기존 교환 안내", new BigDecimal("9090"), "/old.jpg", "A"
        );

        product.applySnapshot(
                "변경된 상품명", new BigDecimal("20000"), new BigDecimal("9000"), ProductStatus.SUSPENDED,
                "변경된 설명", "변경된 결제 안내", "변경된 배송 안내", "변경된 교환 안내", new BigDecimal("8000"), "/new.jpg", "B"
        );

        assertThat(product.getProductName()).isEqualTo("변경된 상품명");
        assertThat(product.getPrice()).isEqualTo(new BigDecimal("20000"));
        assertThat(product.getSupplyPrice()).isEqualTo(new BigDecimal("9000"));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
        assertThat(product.getDescription()).isEqualTo("변경된 설명");
        assertThat(product.getPaymentInfo()).isEqualTo("변경된 결제 안내");
        assertThat(product.getShippingInfo()).isEqualTo("변경된 배송 안내");
        assertThat(product.getExchangeInfo()).isEqualTo("변경된 교환 안내");
        assertThat(product.getPriceExcludingTax()).isEqualTo(new BigDecimal("8000"));
        assertThat(product.getDetailImage()).isEqualTo("/new.jpg");
        assertThat(product.getImageUploadType()).isEqualTo("B");
    }

    @Test
    void markMissing으로_누락_시점을_기록할_수_있다() {
        Product product = Product.register(
                "mymall", 100L, "테스트 상품", new BigDecimal("10000"), new BigDecimal("5000"), ProductStatus.ON_SALE,
                null, null, null, null, null, null, null
        );

        product.markMissing();

        assertThat(product.getMissingSince()).isNotNull();
    }

    @Test
    void applySnapshot을_적용하면_missingSince가_초기화된다() {
        Product product = Product.register(
                "mymall", 100L, "테스트 상품", new BigDecimal("10000"), new BigDecimal("5000"), ProductStatus.ON_SALE,
                null, null, null, null, null, null, null
        );
        product.markMissing();

        product.applySnapshot(
                "테스트 상품", new BigDecimal("10000"), new BigDecimal("5000"), ProductStatus.ON_SALE,
                null, null, null, null, null, null, null
        );

        assertThat(product.getMissingSince()).isNull();
    }
}
