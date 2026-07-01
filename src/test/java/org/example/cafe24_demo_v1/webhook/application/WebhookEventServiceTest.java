package org.example.cafe24_demo_v1.webhook.application;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.benefit.application.service.BenefitService;
import org.example.cafe24_demo_v1.benefit.domain.repository.BenefitWebhookEventRepository;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierWebhookEventRepository;
import org.example.cafe24_demo_v1.order.application.service.OrderWebhookEventService;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.webhook.domain.event.AppUninstalledEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.BenefitCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.BenefitDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.BenefitUpdatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.CarrierUpdatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.OrderCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductUpdatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.model.WebhookEventType;
import org.example.cafe24_demo_v1.webhook.domain.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookEventServiceTest {

    @Mock private AppAuthorizationService authorizationService;
    @Mock private ProductService productService;
    @Mock private BenefitService benefitService;
    @Mock private OrderWebhookEventService orderWebhookEventService;
    @Mock private WebhookEventRepository webhookEventRepository;
    @Mock private CarrierWebhookEventRepository carrierWebhookEventRepository;
    @Mock private BenefitWebhookEventRepository benefitWebhookEventRepository;

    private WebhookEventService webhookEventService;

    @BeforeEach
    void setUp() {
        webhookEventService = new WebhookEventService(
                authorizationService, productService, benefitService, orderWebhookEventService,
                webhookEventRepository, carrierWebhookEventRepository, benefitWebhookEventRepository
        );
    }

    @Test
    void 신규_상품_생성_이벤트는_Cafe24에서_상세를_다시_조회해_저장한다() {
        given(webhookEventRepository.exists(90071, "mymall", "1")).willReturn(false);

        webhookEventService.onProductCreated(new ProductCreatedEvent(90071, "mymall", 1L));

        verify(webhookEventRepository).save(90071, WebhookEventType.PRODUCT_CREATED, "mymall", "1");
        verify(productService).upsertFromWebhook("mymall", 1L);
    }

    @Test
    void 이미_처리한_상품_생성_이벤트는_무시한다() {
        given(webhookEventRepository.exists(90071, "mymall", "1")).willReturn(true);

        webhookEventService.onProductCreated(new ProductCreatedEvent(90071, "mymall", 1L));

        verify(productService, never()).upsertFromWebhook(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 같은_eventNo여도_상품번호가_다르면_중복으로_보지_않는다() {
        given(webhookEventRepository.exists(90071, "mymall", "2")).willReturn(false);

        webhookEventService.onProductCreated(new ProductCreatedEvent(90071, "mymall", 2L));

        verify(productService).upsertFromWebhook("mymall", 2L);
    }

    @Test
    void 앱_삭제_이벤트를_처리하면_인가를_취소한다() {
        given(webhookEventRepository.exists(10, "mymall", null)).willReturn(false);

        webhookEventService.onAppUninstalled(new AppUninstalledEvent(10, "mymall", "client-id"));

        verify(authorizationService).revoke(ArgumentMatchers.any());
    }

    @Test
    void 상품_수정_이벤트는_Cafe24에서_상세를_다시_조회해_저장한다() {
        given(webhookEventRepository.exists(90072, "mymall", "1")).willReturn(false);

        webhookEventService.onProductUpdated(new ProductUpdatedEvent(90072, "mymall", 1L));

        verify(webhookEventRepository).save(90072, WebhookEventType.PRODUCT_UPDATED, "mymall", "1");
        verify(productService).upsertFromWebhook("mymall", 1L);
    }

    @Test
    void 이미_처리한_상품_수정_이벤트는_무시한다() {
        given(webhookEventRepository.exists(90072, "mymall", "1")).willReturn(true);

        webhookEventService.onProductUpdated(new ProductUpdatedEvent(90072, "mymall", 1L));

        verify(productService, never()).upsertFromWebhook(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 상품_삭제_이벤트는_로컬DB에서만_삭제한다() {
        given(webhookEventRepository.exists(90073, "mymall", "1")).willReturn(false);

        webhookEventService.onProductDeleted(new ProductDeletedEvent(90073, "mymall", 1L));

        verify(webhookEventRepository).save(90073, WebhookEventType.PRODUCT_DELETED, "mymall", "1");
        verify(productService).deleteFromWebhook("mymall", 1L);
    }

    @Test
    void 이미_처리한_상품_삭제_이벤트는_무시한다() {
        given(webhookEventRepository.exists(90073, "mymall", "1")).willReturn(true);

        webhookEventService.onProductDeleted(new ProductDeletedEvent(90073, "mymall", 1L));

        verify(productService, never()).deleteFromWebhook(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 배송사_등록_이벤트는_자체_이력_테이블에만_기록한다() {
        given(carrierWebhookEventRepository.exists(90081, "mymall", "01")).willReturn(false);

        webhookEventService.onCarrierCreated(new CarrierCreatedEvent(90081, "mymall", "01"));

        verify(carrierWebhookEventRepository).save(90081, "CARRIER_CREATED", "mymall", "01");
    }

    @Test
    void 이미_처리한_배송사_등록_이벤트는_무시한다() {
        given(carrierWebhookEventRepository.exists(90081, "mymall", "01")).willReturn(true);

        webhookEventService.onCarrierCreated(new CarrierCreatedEvent(90081, "mymall", "01"));

        verify(carrierWebhookEventRepository, never()).save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 배송사_수정_이벤트는_자체_이력_테이블에만_기록한다() {
        given(carrierWebhookEventRepository.exists(90082, "mymall", "01")).willReturn(false);

        webhookEventService.onCarrierUpdated(new CarrierUpdatedEvent(90082, "mymall", "01"));

        verify(carrierWebhookEventRepository).save(90082, "CARRIER_UPDATED", "mymall", "01");
    }

    @Test
    void 이미_처리한_배송사_수정_이벤트는_무시한다() {
        given(carrierWebhookEventRepository.exists(90082, "mymall", "01")).willReturn(true);

        webhookEventService.onCarrierUpdated(new CarrierUpdatedEvent(90082, "mymall", "01"));

        verify(carrierWebhookEventRepository, never()).save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 배송사_삭제_이벤트는_자체_이력_테이블에만_기록한다() {
        given(carrierWebhookEventRepository.exists(90083, "mymall", "01")).willReturn(false);

        webhookEventService.onCarrierDeleted(new CarrierDeletedEvent(90083, "mymall", "01"));

        verify(carrierWebhookEventRepository).save(90083, "CARRIER_DELETED", "mymall", "01");
    }

    @Test
    void 이미_처리한_배송사_삭제_이벤트는_무시한다() {
        given(carrierWebhookEventRepository.exists(90083, "mymall", "01")).willReturn(true);

        webhookEventService.onCarrierDeleted(new CarrierDeletedEvent(90083, "mymall", "01"));

        verify(carrierWebhookEventRepository, never()).save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 주문_생성_이벤트는_원본_저장만_위임한다() {
        webhookEventService.onOrderCreated(new OrderCreatedEvent(90023, "mymall", "1", "{}"));

        verify(orderWebhookEventService).saveRaw("mymall", 90023, "ORDER_CREATED", "1", "{}");
    }

    @Test
    void 혜택_등록_이벤트는_혜택_전용_이력_테이블에_저장하고_Cafe24에서_상세를_재조회한다() {
        given(benefitWebhookEventRepository.exists(90091, "mymall", "1000")).willReturn(false);

        webhookEventService.onBenefitCreated(new BenefitCreatedEvent(90091, "mymall", 1000));

        verify(benefitWebhookEventRepository).save(90091, "BENEFIT_CREATED", "mymall", "1000");
        verify(benefitService).upsertFromWebhook("mymall", 1000);
    }

    @Test
    void 이미_처리한_혜택_등록_이벤트는_무시한다() {
        given(benefitWebhookEventRepository.exists(90091, "mymall", "1000")).willReturn(true);

        webhookEventService.onBenefitCreated(new BenefitCreatedEvent(90091, "mymall", 1000));

        verify(benefitService, never()).upsertFromWebhook(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 혜택_수정_이벤트는_혜택_전용_이력_테이블에_저장하고_Cafe24에서_상세를_재조회한다() {
        given(benefitWebhookEventRepository.exists(90092, "mymall", "1000")).willReturn(false);

        webhookEventService.onBenefitUpdated(new BenefitUpdatedEvent(90092, "mymall", 1000));

        verify(benefitWebhookEventRepository).save(90092, "BENEFIT_UPDATED", "mymall", "1000");
        verify(benefitService).upsertFromWebhook("mymall", 1000);
    }

    @Test
    void 이미_처리한_혜택_수정_이벤트는_무시한다() {
        given(benefitWebhookEventRepository.exists(90092, "mymall", "1000")).willReturn(true);

        webhookEventService.onBenefitUpdated(new BenefitUpdatedEvent(90092, "mymall", 1000));

        verify(benefitService, never()).upsertFromWebhook(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void 혜택_삭제_이벤트는_혜택_전용_이력_테이블에_저장하고_로컬DB에서_혜택을_제거한다() {
        given(benefitWebhookEventRepository.exists(90093, "mymall", "1000")).willReturn(false);

        webhookEventService.onBenefitDeleted(new BenefitDeletedEvent(90093, "mymall", 1000));

        verify(benefitWebhookEventRepository).save(90093, "BENEFIT_DELETED", "mymall", "1000");
        verify(benefitService).deleteFromWebhook("mymall", 1000);
    }

    @Test
    void 이미_처리한_혜택_삭제_이벤트는_무시한다() {
        given(benefitWebhookEventRepository.exists(90093, "mymall", "1000")).willReturn(true);

        webhookEventService.onBenefitDeleted(new BenefitDeletedEvent(90093, "mymall", 1000));

        verify(benefitService, never()).deleteFromWebhook(ArgumentMatchers.any(), ArgumentMatchers.any());
    }
}
