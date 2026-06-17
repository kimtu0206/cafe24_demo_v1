package org.example.cafe24_demo_v1.webhook.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.command.RevokeAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.webhook.domain.event.AppUninstalledEvent;
import org.example.cafe24_demo_v1.webhook.infrastructure.persistence.WebhookEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Webhook 도메인 이벤트를 구독해 처리하는 애플리케이션 서비스.
 *
 * WebhookController가 이벤트를 발행하면, Spring이 @EventListener 메서드를 자동으로 호출한다.
 * 이 방식 덕분에 WebhookController는 "무슨 일이 일어났는지" 만 알리고,
 * "그 결과로 무엇을 해야 하는지" 는 이 클래스가 독립적으로 결정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventService {

    private final AppAuthorizationService authorizationService;
    private final WebhookEventRepository webhookEventRepository;

    /**
     * 앱 삭제 이벤트 처리기.
     * eventNo + mallId 조합으로 중복 수신을 확인한 뒤, 최초 수신 시에만 인가를 REVOKED로 변경한다.
     * Cafe24는 네트워크 상황에 따라 동일 이벤트를 여러 번 전송할 수 있다.
     */
    @Transactional
    @EventListener
    public void onAppUninstalled(AppUninstalledEvent event) {
        // 이미 처리한 이벤트면 무시 (멱등성 보장)
        if (webhookEventRepository.existsByEventNoAndMallId(event.getEventNo(), event.getMallId())) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}", event.getEventNo(), event.getMallId());
            return;
        }

        // 이력 저장 (이후 중복 수신 시 위 조건에서 걸림)
        webhookEventRepository.save(event.getEventNo(), event.getMallId());

        log.info("App uninstalled: mallId={}, clientId={}", event.getMallId(), event.getClientId());
        authorizationService.revoke(new RevokeAuthorizationCommand(event.getMallId()));
    }
}
