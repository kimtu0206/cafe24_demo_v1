package org.example.cafe24_demo_v1.webhook.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.webhook.domain.event.AppUninstalledEvent;
import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Cafe24 앱 삭제(설치 해제) Webhook 수신 컨트롤러.
 * Cafe24 개발자센터에서 이 엔드포인트를 앱 삭제 이벤트 전용 Webhook URL로 등록해야 한다.
 */
@Tag(name = "Webhook", description = "Cafe24 Webhook 수신 엔드포인트 — x-api-key 헤더 인증 필요")
@SecurityRequirement(name = "WebhookApiKey")
@Slf4j
@RestController
@RequestMapping("/webhook/cafe24/app-uninstalled")
public class AppUninstallWebhookController extends AbstractCafe24WebhookController {

    private final ApplicationEventPublisher eventPublisher;

    public AppUninstallWebhookController(Cafe24WebhookVerifier verifier, ApplicationEventPublisher eventPublisher) {
        super(verifier);
        this.eventPublisher = eventPublisher;
    }

    @Operation(summary = "앱 삭제 Webhook 수신", description = "Cafe24 앱 삭제 이벤트를 수신합니다. 검증 통과 시 인가 정보를 revoke합니다.")
    @PostMapping
    public ResponseEntity<Void> receive(
            @Parameter(hidden = true) @RequestHeader Map<String, String> headers,
            @RequestBody Cafe24WebhookPayload payload) {

        return reject(headers, payload.eventNo(), payload.resource()).orElseGet(() -> {
            log.info("Webhook received: eventNo={}, mallId={}", payload.eventNo(), payload.resource().mallId());
            eventPublisher.publishEvent(
                    new AppUninstalledEvent(payload.eventNo(), payload.resource().mallId(), payload.resource().clientId())
            );
            return ResponseEntity.ok().build();
        });
    }
}
