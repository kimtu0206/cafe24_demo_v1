package org.example.cafe24_demo_v1.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class WorkerPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void worker_order_webhook_프로퍼티가_바인딩된다() {
        contextRunner
                .withPropertyValues(
                        "worker.order-webhook.fixed-delay-ms=60000",
                        "worker.order-webhook.batch-size=50",
                        "worker.order-webhook.max-retry-count=5"
                )
                .run(context -> {
                    WorkerProperties properties = context.getBean(WorkerProperties.class);
                    assertThat(properties.getOrderWebhook().getFixedDelayMs()).isEqualTo(60000L);
                    assertThat(properties.getOrderWebhook().getBatchSize()).isEqualTo(50);
                    assertThat(properties.getOrderWebhook().getMaxRetryCount()).isEqualTo(5);
                });
    }

    @EnableConfigurationProperties(WorkerProperties.class)
    @Configuration
    static class TestConfig {}
}
