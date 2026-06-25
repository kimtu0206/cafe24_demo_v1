package org.example.cafe24_demo_v1.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SyncPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void sync_order_product_carrier_프로퍼티가_바인딩된다() {
        contextRunner
                .withPropertyValues(
                        "sync.order.fixed-rate-ms=86400000",
                        "sync.order.lookback-hours=48",
                        "sync.product.cron=0 0 10 * * *",
                        "sync.carrier.cron=0 30 16 * * *"
                )
                .run(context -> {
                    SyncProperties properties = context.getBean(SyncProperties.class);
                    assertThat(properties.getOrder().getFixedRateMs()).isEqualTo(86400000L);
                    assertThat(properties.getOrder().getLookbackHours()).isEqualTo(48L);
                    assertThat(properties.getProduct().getCron()).isEqualTo("0 0 10 * * *");
                    assertThat(properties.getCarrier().getCron()).isEqualTo("0 30 16 * * *");
                });
    }

    @EnableConfigurationProperties(SyncProperties.class)
    @Configuration
    static class TestConfig {}
}
