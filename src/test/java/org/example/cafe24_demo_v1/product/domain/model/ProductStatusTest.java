package org.example.cafe24_demo_v1.product.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductStatusTest {

    @Test
    void 진열중이고_판매중이면_ON_SALE이다() {
        assertThat(ProductStatus.from("T", "T")).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void 진열되지_않으면_SUSPENDED이다() {
        assertThat(ProductStatus.from("F", "T")).isEqualTo(ProductStatus.SUSPENDED);
    }

    @Test
    void 판매중이_아니면_SUSPENDED이다() {
        assertThat(ProductStatus.from("T", "F")).isEqualTo(ProductStatus.SUSPENDED);
    }

    @Test
    void 둘다_아니면_SUSPENDED이다() {
        assertThat(ProductStatus.from("F", "F")).isEqualTo(ProductStatus.SUSPENDED);
    }
}
