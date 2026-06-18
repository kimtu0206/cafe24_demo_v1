package org.example.cafe24_demo_v1.webhook.application;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.product.application.service.ProductService;
import org.example.cafe24_demo_v1.webhook.domain.event.AppUninstalledEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductCreatedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductDeletedEvent;
import org.example.cafe24_demo_v1.webhook.domain.event.ProductUpdatedEvent;
import org.example.cafe24_demo_v1.webhook.infrastructure.persistence.WebhookEventRepository;
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
    @Mock private WebhookEventRepository webhookEventRepository;

    private WebhookEventService webhookEventService;

    @BeforeEach
    void setUp() {
        webhookEventService = new WebhookEventService(authorizationService, productService, webhookEventRepository);
    }

    @Test
    void 신규_상품_생성_이벤트는_Cafe24에서_상세를_다시_조회해_저장한다() {
        given(webhookEventRepository.exists(90071, "mymall", "1")).willReturn(false);

        webhookEventService.onProductCreated(new ProductCreatedEvent(90071, "mymall", 1L));

        verify(webhookEventRepository).save(90071, "mymall", "1");
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

        verify(webhookEventRepository).save(90072, "mymall", "1");
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

        verify(webhookEventRepository).save(90073, "mymall", "1");
        verify(productService).deleteFromWebhook(1L);
    }

    @Test
    void 이미_처리한_상품_삭제_이벤트는_무시한다() {
        given(webhookEventRepository.exists(90073, "mymall", "1")).willReturn(true);

        webhookEventService.onProductDeleted(new ProductDeletedEvent(90073, "mymall", 1L));

        verify(productService, never()).deleteFromWebhook(ArgumentMatchers.any());
    }
}
