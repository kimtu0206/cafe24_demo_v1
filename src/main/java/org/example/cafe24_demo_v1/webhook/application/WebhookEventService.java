package org.example.cafe24_demo_v1.webhook.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.command.RevokeAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.webhook.domain.event.AppUninstalledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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

    /**
     * 앱 삭제 이벤트 처리기.
     * 해당 쇼핑몰의 인가를 REVOKED 상태로 변경한다.
     */
    @EventListener
    public void onAppUninstalled(AppUninstalledEvent event) {
        log.info("App uninstalled: mallId={}, clientId={}", event.getMallId(), event.getClientId());
        authorizationService.revoke(new RevokeAuthorizationCommand(event.getMallId()));
    }
}
