package org.example.cafe24_demo_v1.carrier.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarrierTest {

    @Test
    void register로_생성하면_입력한_값으로_초기화된다() {
        Carrier carrier = Carrier.register(
                "mymall", 10L, "01", "우체국", "1588-1300", "02-1234-5678", "contact@example.com",
                "https://track.example.com", new BigDecimal("2500"), "https://carrier.example.com",
                ShippingType.DOMESTIC, true, true
        );

        assertThat(carrier.getMallId()).isEqualTo("mymall");
        assertThat(carrier.getCarrierId()).isEqualTo(10L);
        assertThat(carrier.getShippingCarrierCode()).isEqualTo("01");
        assertThat(carrier.getShippingCarrierName()).isEqualTo("우체국");
        assertThat(carrier.getContact()).isEqualTo("1588-1300");
        assertThat(carrier.getSecondaryContact()).isEqualTo("02-1234-5678");
        assertThat(carrier.getEmail()).isEqualTo("contact@example.com");
        assertThat(carrier.getTrackShipmentUrl()).isEqualTo("https://track.example.com");
        assertThat(carrier.getDefaultShippingFee()).isEqualTo(new BigDecimal("2500"));
        assertThat(carrier.getHomepageUrl()).isEqualTo("https://carrier.example.com");
        assertThat(carrier.getShippingType()).isEqualTo(ShippingType.DOMESTIC);
        assertThat(carrier.isDefaultCarrier()).isTrue();
        assertThat(carrier.isShippingFeeSetting()).isTrue();
        assertThat(carrier.getCreatedAt()).isNotNull();
    }

    @Test
    void applySnapshot으로_배송사_정보를_갱신할_수_있다() {
        Carrier carrier = Carrier.register(
                "mymall", 10L, "01", "기존 배송사명", "1588-0000", "02-0000-0000", "old@example.com",
                "https://old-track.example.com", new BigDecimal("2500"), "https://old.example.com",
                ShippingType.DOMESTIC, true, true
        );

        carrier.applySnapshot(
                "변경된 배송사명", "1588-9999", "02-9999-9999", "new@example.com",
                "https://new-track.example.com", new BigDecimal("3000"), "https://new.example.com",
                ShippingType.INTERNATIONAL, false, false
        );

        assertThat(carrier.getShippingCarrierName()).isEqualTo("변경된 배송사명");
        assertThat(carrier.getContact()).isEqualTo("1588-9999");
        assertThat(carrier.getSecondaryContact()).isEqualTo("02-9999-9999");
        assertThat(carrier.getEmail()).isEqualTo("new@example.com");
        assertThat(carrier.getTrackShipmentUrl()).isEqualTo("https://new-track.example.com");
        assertThat(carrier.getDefaultShippingFee()).isEqualTo(new BigDecimal("3000"));
        assertThat(carrier.getHomepageUrl()).isEqualTo("https://new.example.com");
        assertThat(carrier.getShippingType()).isEqualTo(ShippingType.INTERNATIONAL);
        assertThat(carrier.isDefaultCarrier()).isFalse();
        assertThat(carrier.isShippingFeeSetting()).isFalse();
        // 생성 후 불변 필드는 그대로 유지된다
        assertThat(carrier.getShippingCarrierCode()).isEqualTo("01");
    }

    @Test
    void register시_배송사명이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> Carrier.register(
                "mymall", 10L, "01", null, "1588-1300", "02-1234-5678", "contact@example.com",
                "https://track.example.com", new BigDecimal("2500"), "https://carrier.example.com",
                ShippingType.DOMESTIC, true, true
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void register시_배송타입이_null이면_예외가_발생한다() {
        assertThatThrownBy(() -> Carrier.register(
                "mymall", 10L, "01", "우체국", "1588-1300", "02-1234-5678", "contact@example.com",
                "https://track.example.com", new BigDecimal("2500"), "https://carrier.example.com",
                null, true, true
        )).isInstanceOf(NullPointerException.class);
    }
}
