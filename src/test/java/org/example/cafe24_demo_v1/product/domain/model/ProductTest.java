package org.example.cafe24_demo_v1.product.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void register로_생성하면_입력한_값으로_초기화된다() {
        Product product = Product.register(
                "mymall", 100L, "테스트 상품", new BigDecimal("10000"), new BigDecimal("5000"), ProductStatus.ON_SALE
        );

        assertThat(product.getMallId()).isEqualTo("mymall");
        assertThat(product.getProductNo()).isEqualTo(100L);
        assertThat(product.getProductName()).isEqualTo("테스트 상품");
        assertThat(product.getPrice()).isEqualTo(new BigDecimal("10000"));
        assertThat(product.getSupplyPrice()).isEqualTo(new BigDecimal("5000"));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(product.getCreatedAt()).isNotNull();
    }

    @Test
    void applySnapshot으로_상품_정보를_갱신할_수_있다() {
        Product product = Product.register(
                "mymall", 100L, "기존 상품명", new BigDecimal("10000"), new BigDecimal("5000"), ProductStatus.ON_SALE
        );

        product.applySnapshot("변경된 상품명", new BigDecimal("20000"), new BigDecimal("9000"), ProductStatus.SUSPENDED);

        assertThat(product.getProductName()).isEqualTo("변경된 상품명");
        assertThat(product.getPrice()).isEqualTo(new BigDecimal("20000"));
        assertThat(product.getSupplyPrice()).isEqualTo(new BigDecimal("9000"));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SUSPENDED);
    }
}
