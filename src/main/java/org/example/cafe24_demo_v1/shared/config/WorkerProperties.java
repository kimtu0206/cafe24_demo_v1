package org.example.cafe24_demo_v1.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private OrderWebhook orderWebhook = new OrderWebhook();

    @Getter
    @Setter
    public static class OrderWebhook {
        private long fixedDelayMs;
        private int batchSize;
        private int maxRetryCount;
    }
}
