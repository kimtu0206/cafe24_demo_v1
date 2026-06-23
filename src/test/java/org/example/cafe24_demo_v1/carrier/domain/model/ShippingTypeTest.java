package org.example.cafe24_demo_v1.carrier.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShippingTypeTest {

    @Test
    void 코드_A는_DOMESTIC이다() {
        assertThat(ShippingType.from("A")).isEqualTo(ShippingType.DOMESTIC);
    }

    @Test
    void 코드_B는_DOMESTIC_AND_INTERNATIONAL이다() {
        assertThat(ShippingType.from("B")).isEqualTo(ShippingType.DOMESTIC_AND_INTERNATIONAL);
    }

    @Test
    void 코드_C는_INTERNATIONAL이다() {
        assertThat(ShippingType.from("C")).isEqualTo(ShippingType.INTERNATIONAL);
    }

    @Test
    void 코드_F는_NOT_SET이다() {
        assertThat(ShippingType.from("F")).isEqualTo(ShippingType.NOT_SET);
    }

    @Test
    void 코드가_null이면_NOT_SET이다() {
        assertThat(ShippingType.from(null)).isEqualTo(ShippingType.NOT_SET);
    }

    @Test
    void toCode로_다시_코드로_변환할_수_있다() {
        assertThat(ShippingType.DOMESTIC.toCode()).isEqualTo("A");
        assertThat(ShippingType.DOMESTIC_AND_INTERNATIONAL.toCode()).isEqualTo("B");
        assertThat(ShippingType.INTERNATIONAL.toCode()).isEqualTo("C");
        assertThat(ShippingType.NOT_SET.toCode()).isEqualTo("F");
    }

    @Test
    void 알수없는_코드는_예외를_던진다() {
        assertThatThrownBy(() -> ShippingType.from("Z"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
