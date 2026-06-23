package org.example.cafe24_demo_v1.webhook.presentation;

import org.example.cafe24_demo_v1.webhook.domain.event.CarrierCreatedEvent;
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
class CarrierWebhookControllerTest {

    @Mock private Cafe24WebhookVerifier verifier;
    @Mock private ApplicationEventPublisher eventPublisher;

    private CarrierWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new CarrierWebhookController(verifier, eventPublisher);
        given(verifier.verify("valid-key")).willReturn(true);
    }

    @Test
    void shippingCarrierCode가_없으면_400을_반환하고_이벤트를_발행하지_않는다() {
        Cafe24WebhookPayload payload = new Cafe24WebhookPayload(
                90081, new Cafe24WebhookPayload.Resource("mymall", null, null, null, null, null)
        );

        ResponseEntity<Void> response = controller.created(headers(), payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shippingCarrierCode가_있으면_정상적으로_이벤트를_발행한다() {
        Cafe24WebhookPayload payload = new Cafe24WebhookPayload(
                90081, new Cafe24WebhookPayload.Resource("mymall", null, null, null, null, "01")
        );

        ResponseEntity<Void> response = controller.created(headers(), payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<CarrierCreatedEvent> captor = ArgumentCaptor.forClass(CarrierCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getEventNo()).isEqualTo(90081);
        assertThat(captor.getValue().getMallId()).isEqualTo("mymall");
        assertThat(captor.getValue().getShippingCarrierCode()).isEqualTo("01");
    }

    private Map<String, String> headers() {
        return Map.of("x-api-key", "valid-key");
    }
}
