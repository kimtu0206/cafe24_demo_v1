package org.example.cafe24_demo_v1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.entity.Cafe24Token;
import org.example.cafe24_demo_v1.repository.Cafe24TokenRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Cafe24WebhookService {

    private final Cafe24Properties cafe24Properties;
    private final Cafe24TokenRepository tokenRepository;

    private static final List<String> EVENT_TYPES = List.of(
            "product_created",
            "product_updated",
            "order_created",
            "category_created"
    );

    public void registerAllWebhooks() {
        Cafe24Token token = tokenRepository
                .findByMallIdAndClientId(
                        cafe24Properties.getMallId(),
                        cafe24Properties.getClientId()
                )
                .orElseThrow(() -> new IllegalStateException("토큰이 없습니다. 먼저 OAuth 인증을 완료해주세요."));

        for (String eventType : EVENT_TYPES) {
            registerWebhook(token.getAccessToken(), eventType);
        }
    }

    private void registerWebhook(String accessToken, String eventType) {
        String url = "https://" + cafe24Properties.getMallId()
                + ".cafe24api.com/api/v2/webhooks";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Cafe24-Api-Version", "2022-09-01");

        Map<String, Object> body = Map.of(
                "shop_no", 1,
                "webhooks", Map.of(
                        "event_type", eventType,
                        "callback_url", cafe24Properties.getWebhook().getCallbackUrl()
                )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            new RestTemplate().postForEntity(url, request, String.class);
            log.info("웹훅 등록 성공: {}", eventType);
        } catch (Exception e) {
            log.error("웹훅 등록 실패: {} - {}", eventType, e.getMessage());
            throw new IllegalStateException("웹훅 등록 실패: " + eventType, e);
        }
    }
}
