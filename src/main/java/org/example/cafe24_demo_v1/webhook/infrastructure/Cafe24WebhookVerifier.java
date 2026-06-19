package org.example.cafe24_demo_v1.webhook.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.stereotype.Component;

/**
 * Cafe24 Webhook 요청의 서명을 검증하는 인프라 컴포넌트.
 *
 * Cafe24는 x-api-key 헤더에 API Key 원문을 담아 전송한다.
 */
@Component
@RequiredArgsConstructor
public class Cafe24WebhookVerifier {

    private final Cafe24Properties cafe24Properties;

    /**
     * 요청 헤더의 x-api-key 값이 설정된 API Key와 일치하는지 확인한다.
     * API Key가 설정되지 않은 경우 검증을 통과시키지 않고 실패로 처리한다(fail-closed).
     *
     * @param apiKeyHeader Webhook 요청의 x-api-key 헤더 값
     * @return 검증 성공 시 true, 실패 시 false
     */
    public boolean verify(String apiKeyHeader) {
        String expectedKey = cafe24Properties.getWebhook().getApiKey();
        if (expectedKey == null || expectedKey.isBlank()) {
            return false;
        }
        return expectedKey.equals(apiKeyHeader);
    }
}
