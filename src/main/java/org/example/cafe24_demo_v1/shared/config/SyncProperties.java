package org.example.cafe24_demo_v1.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sync")
public class SyncProperties {

    private Order order = new Order();
    private Product product = new Product();
    private Carrier carrier = new Carrier();

    @Getter
    @Setter
    public static class Order {
        private long fixedRateMs;
        private long lookbackHours;
    }

    @Getter
    @Setter
    public static class Product {
        private String cron;
    }

    @Getter
    @Setter
    public static class Carrier {
        private String cron;
    }
}
