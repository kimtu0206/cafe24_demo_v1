package org.example.cafe24_demo_v1.webhook.presentation;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Cafe24WebhookPayloadTest {

    // 운영에서 쓰는 Spring Boot 자동 설정 ObjectMapper와 동일하게 알 수 없는 필드는 무시하도록 맞춘다.
    private final ObjectMapper objectMapper =
            new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** Cafe24가 실제로 보내는 배송사 등록 Webhook은 REST API와 달리 sc_id 키로 배송사 코드를 전달한다. */
    @Test
    void 배송사_등록_웹훅의_sc_id를_shippingCarrierCode로_역직렬화한다() throws Exception {
        String json = """
                {
                    "event_no": 90100,
                    "resource": {
                        "mall_id": "cafe24bestshop",
                        "sc_id": "3",
                        "sc_name": "FASTBOX"
                    }
                }
                """;

        Cafe24WebhookPayload payload = objectMapper.readValue(json, Cafe24WebhookPayload.class);

        assertThat(payload.eventNo()).isEqualTo(90100);
        assertThat(payload.resource().mallId()).isEqualTo("cafe24bestshop");
        assertThat(payload.resource().shippingCarrierCode()).isEqualTo("3");
    }
}
