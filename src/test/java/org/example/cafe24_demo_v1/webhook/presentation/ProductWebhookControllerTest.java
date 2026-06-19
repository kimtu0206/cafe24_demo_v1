package org.example.cafe24_demo_v1.webhook.presentation;

import org.example.cafe24_demo_v1.webhook.domain.event.ProductCreatedEvent;
import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductWebhookControllerTest {

    @Mock private Cafe24WebhookVerifier verifier;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ProductWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductWebhookController(verifier, eventPublisher);
        given(verifier.verify("valid-key")).willReturn(true);
    }

    @Test
    void productNo가_없으면_400을_반환하고_이벤트를_발행하지_않는다() {
        Cafe24WebhookPayload payload = new Cafe24WebhookPayload(
                90071, new Cafe24WebhookPayload.Resource("mymall", null, null, null, null)
        );

        ResponseEntity<Void> response = controller.created(headers(), payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void productNo가_있으면_정상적으로_이벤트를_발행한다() {
        Cafe24WebhookPayload payload = new Cafe24WebhookPayload(
                90071, new Cafe24WebhookPayload.Resource("mymall", null, null, null, 1L)
        );

        ResponseEntity<Void> response = controller.created(headers(), payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<ProductCreatedEvent> captor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getEventNo()).isEqualTo(90071);
        assertThat(captor.getValue().getMallId()).isEqualTo("mymall");
        assertThat(captor.getValue().getProductNo()).isEqualTo(1L);
    }

    private Map<String, String> headers() {
        return Map.of("x-api-key", "valid-key");
    }
}
