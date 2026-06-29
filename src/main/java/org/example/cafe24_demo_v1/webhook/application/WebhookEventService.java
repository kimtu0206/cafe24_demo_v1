package org.example.cafe24_demo_v1.webhook.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.command.RevokeAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitWebhookEventRepository;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierWebhookEventRepository;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.webhook.domain.event.AppUninstalledEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.BenefitCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierUpdatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.OrderCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductUpdatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.model.WebhookEventType;
import org.example.cafe24_demo_v1.webhook.domain.repository.WebhookEventRepository;
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
    private final ProductService productService;
    private final BenefitService benefitService;
    private final OrderWebhookEventService orderWebhookEventService;
    private final WebhookEventRepository webhookEventRepository;
    private final CarrierWebhookEventRepository carrierWebhookEventRepository;
    private final BenefitWebhookEventRepository benefitWebhookEventRepository;

    /**
     * 앱 삭제 이벤트 처리기.
     * eventNo + mallId 조합으로 중복 수신을 확인한 뒤, 최초 수신 시에만 인가를 REVOKED로 변경한다.
     * Cafe24는 네트워크 상황에 따라 동일 이벤트를 여러 번 전송할 수 있다.
     */
    @Transactional
    @EventListener
    public void onAppUninstalled(AppUninstalledEvent event) {
        // 이미 처리한 이벤트면 무시 (멱등성 보장)
        if (webhookEventRepository.exists(event.getEventNo(), event.getMallId(), null)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}", event.getEventNo(), event.getMallId());
            return;
        }

        // 이력 저장 (이후 중복 수신 시 위 조건에서 걸림)
        webhookEventRepository.save(event.getEventNo(), WebhookEventType.APP_UNINSTALLED, event.getMallId(), null);

        log.info("App uninstalled: mallId={}, clientId={}", event.getMallId(), event.getClientId());
        authorizationService.revoke(new RevokeAuthorizationCommand(event.getMallId()));
    }

    /**
     * 상품 생성 이벤트 처리기.
     * eventNo + mallId + productNo 조합으로 중복 수신을 확인한 뒤,
     * 최초 수신 시에만 Cafe24에서 상품 상세를 다시 조회해 로컬 DB에 반영한다.
     */
    @Transactional
    @EventListener
    public void onProductCreated(ProductCreatedEvent event) {
        String resourceId = String.valueOf(event.getProductNo());

        if (webhookEventRepository.exists(event.getEventNo(), event.getMallId(), resourceId)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}, productNo={}",
                    event.getEventNo(), event.getMallId(), event.getProductNo());
            return;
        }

        webhookEventRepository.save(event.getEventNo(), WebhookEventType.PRODUCT_CREATED, event.getMallId(), resourceId);

        log.info("Product created: mallId={}, productNo={}", event.getMallId(), event.getProductNo());
        productService.upsertFromWebhook(event.getMallId(), event.getProductNo());
    }

    /**
     * 상품 수정 이벤트 처리기.
     * eventNo + mallId + productNo 조합으로 중복 수신을 확인한 뒤,
     * 최초 수신 시에만 Cafe24에서 상품 상세를 다시 조회해 로컬 DB에 반영한다(상품 생성과 동일한 처리).
     */
    @Transactional
    @EventListener
    public void onProductUpdated(ProductUpdatedEvent event) {
        String resourceId = String.valueOf(event.getProductNo());

        if (webhookEventRepository.exists(event.getEventNo(), event.getMallId(), resourceId)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}, productNo={}",
                    event.getEventNo(), event.getMallId(), event.getProductNo());
            return;
        }

        webhookEventRepository.save(event.getEventNo(), WebhookEventType.PRODUCT_UPDATED, event.getMallId(), resourceId);

        log.info("Product updated: mallId={}, productNo={}", event.getMallId(), event.getProductNo());
        productService.upsertFromWebhook(event.getMallId(), event.getProductNo());
    }

    /**
     * 상품 삭제 이벤트 처리기.
     * eventNo + mallId + productNo 조합으로 중복 수신을 확인한 뒤,
     * 최초 수신 시에만 로컬 DB에서 상품을 삭제한다.
     * Cafe24에는 이미 삭제된 상태이므로 다시 API를 호출하지 않는다.
     */
    @Transactional
    @EventListener
    public void onProductDeleted(ProductDeletedEvent event) {
        String resourceId = String.valueOf(event.getProductNo());

        if (webhookEventRepository.exists(event.getEventNo(), event.getMallId(), resourceId)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}, productNo={}",
                    event.getEventNo(), event.getMallId(), event.getProductNo());
            return;
        }

        webhookEventRepository.save(event.getEventNo(), WebhookEventType.PRODUCT_DELETED, event.getMallId(), resourceId);

        log.info("Product deleted: mallId={}, productNo={}", event.getMallId(), event.getProductNo());
        productService.deleteFromWebhook(event.getMallId(), event.getProductNo());
    }

    /**
     * 배송사 등록 이벤트 처리기.
     * eventNo + mallId + shippingCarrierCode 조합으로 중복 수신을 확인해 carrier 컨텍스트의
     * 자체 이력 테이블(cafe24_carrier_webhook_event)에만 기록한다(상품/앱과 분리).
     * 실제 cafe24_carrier 테이블 반영은 CarrierSyncScheduler의 주기 동기화가 전담한다.
     */
    @Transactional
    @EventListener
    public void onCarrierCreated(CarrierCreatedEvent event) {
        String resourceId = event.getShippingCarrierCode();

        if (carrierWebhookEventRepository.exists(event.getEventNo(), event.getMallId(), resourceId)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}, shippingCarrierCode={}",
                    event.getEventNo(), event.getMallId(), resourceId);
            return;
        }

        carrierWebhookEventRepository.save(event.getEventNo(), WebhookEventType.CARRIER_CREATED.name(), event.getMallId(), resourceId);
        log.info("Carrier created: mallId={}, shippingCarrierCode={}", event.getMallId(), resourceId);
    }

    /**
     * 배송사 수정 이벤트 처리기. 배송사 등록과 동일하게 이력만 기록한다.
     */
    @Transactional
    @EventListener
    public void onCarrierUpdated(CarrierUpdatedEvent event) {
        String resourceId = event.getShippingCarrierCode();

        if (carrierWebhookEventRepository.exists(event.getEventNo(), event.getMallId(), resourceId)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}, shippingCarrierCode={}",
                    event.getEventNo(), event.getMallId(), resourceId);
            return;
        }

        carrierWebhookEventRepository.save(event.getEventNo(), WebhookEventType.CARRIER_UPDATED.name(), event.getMallId(), resourceId);
        log.info("Carrier updated: mallId={}, shippingCarrierCode={}", event.getMallId(), resourceId);
    }

    /**
     * 배송사 삭제 이벤트 처리기. 배송사 등록과 동일하게 이력만 기록한다.
     */
    @Transactional
    @EventListener
    public void onCarrierDeleted(CarrierDeletedEvent event) {
        String resourceId = event.getShippingCarrierCode();

        if (carrierWebhookEventRepository.exists(event.getEventNo(), event.getMallId(), resourceId)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}, shippingCarrierCode={}",
                    event.getEventNo(), event.getMallId(), resourceId);
            return;
        }

        carrierWebhookEventRepository.save(event.getEventNo(), WebhookEventType.CARRIER_DELETED.name(), event.getMallId(), resourceId);
        log.info("Carrier deleted: mallId={}, shippingCarrierCode={}", event.getMallId(), resourceId);
    }

    /**
     * 혜택 등록 이벤트 처리기.
     * eventNo + mallId + benefitNo 조합으로 중복 수신을 확인한 뒤,
     * 최초 수신 시에만 Cafe24에서 혜택 상세를 다시 조회해 로컬 DB에 반영한다.
     */
    @Transactional
    @EventListener
    public void onBenefitCreated(BenefitCreatedEvent event) {
        String resourceId = String.valueOf(event.getBenefitNo());

        if (benefitWebhookEventRepository.exists(event.getEventNo(), event.getMallId(), resourceId)) {
            log.info("Duplicate webhook ignored: eventNo={}, mallId={}, benefitNo={}",
                    event.getEventNo(), event.getMallId(), event.getBenefitNo());
            return;
        }

        benefitWebhookEventRepository.save(event.getEventNo(), WebhookEventType.BENEFIT_CREATED.name(), event.getMallId(), resourceId);

        log.info("Benefit created: mallId={}, benefitNo={}", event.getMallId(), event.getBenefitNo());
        benefitService.upsertFromWebhook(event.getMallId(), event.getBenefitNo());
    }

    /**
     * 주문 생성 이벤트 처리기.
     * 다른 이벤트와 달리 여기서는 원본 payload 저장만 위임하고 끝낸다(Cafe24 재조회 없음).
     * 멱등성 체크와 실제 order 테이블 반영은 order 컨텍스트가 자체 테이블(cafe24_order_webhook_event)로
     * 전담하므로, 이 메서드는 단순히 위임만 한다.
     */
    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Order created: mallId={}, orderId={}", event.getMallId(), event.getOrderId());
        orderWebhookEventService.saveRaw(event.getMallId(), event.getEventNo(), event.getOrderId(), event.getPayload());
    }
}
