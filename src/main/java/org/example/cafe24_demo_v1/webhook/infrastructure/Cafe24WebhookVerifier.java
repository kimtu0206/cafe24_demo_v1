package org.example.cafe24_demo_v1.webhook.infrastructure;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.stereotype.Component;

/**
 * Cafe24 Webhook 요청의 서명을 검증하는 인프라 컴포넌트.
 *
 * Cafe24는 Webhook 요청 헤더(x-cafe24-signature)에 API Key를 담아 전송한다.
 * 이 값과 설정 파일의 apiKey를 비교해 Cafe24에서 보낸 요청인지 확인한다.
 *
 * apiKey가 설정되지 않은 개발 환경에서는 검증을 건너뛴다.
 */
@Component
@RequiredArgsConstructor
public class Cafe24WebhookVerifier {

    private final Cafe24Properties cafe24Properties;

    /**
     * 요청 헤더의 서명값이 설정된 API Key와 일치하는지 확인한다.
     *
     * @param hmacHeader Webhook 요청의 x-cafe24-signature 헤더 값
     * @return 검증 성공 시 true, 실패 시 false
     */
    public boolean verify(String hmacHeader) {
        String expectedKey = cafe24Properties.getWebhook().getApiKey();
        if (expectedKey == null || expectedKey.isBlank()) {
            return true; // apiKey 미설정 시 개발 편의를 위해 통과
        }
        return expectedKey.equals(hmacHeader);
    }
}
