package org.example.cafe24_demo_v1.webhook.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cafe24_demo_v1.webhook.domain.event.OrderCreatedEvent;
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
class OrderWebhookControllerTest {

    @Mock private Cafe24WebhookVerifier verifier;
    @Mock private ApplicationEventPublisher eventPublisher;

    private OrderWebhookController controller;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        controller = new OrderWebhookController(verifier, eventPublisher);
    }

    @Test
    void order_id가_없으면_400을_반환하고_이벤트를_발행하지_않는다() throws Exception {
        given(verifier.verify("valid-key")).willReturn(true);
        JsonNode resource = objectMapper.readTree("{\"mall_id\": \"mymall\"}");
        OrderWebhookPayload payload = new OrderWebhookPayload(90023, resource);

        ResponseEntity<Void> response = controller.created(headers(), payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void order_id가_있으면_정상적으로_이벤트를_발행한다() throws Exception {
        given(verifier.verify("valid-key")).willReturn(true);
        JsonNode resource = objectMapper.readTree("{\"mall_id\": \"mymall\", \"order_id\": \"20200717-0029236\"}");
        OrderWebhookPayload payload = new OrderWebhookPayload(90023, resource);

        ResponseEntity<Void> response = controller.created(headers(), payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getEventNo()).isEqualTo(90023);
        assertThat(captor.getValue().getMallId()).isEqualTo("mymall");
        assertThat(captor.getValue().getOrderId()).isEqualTo("20200717-0029236");
        assertThat(captor.getValue().getPayload()).contains("20200717-0029236");
    }

    @Test
    void 서명검증에_실패하면_401을_반환한다() throws Exception {
        given(verifier.verify("invalid-key")).willReturn(false);
        JsonNode resource = objectMapper.readTree("{\"mall_id\": \"mymall\", \"order_id\": \"1\"}");
        OrderWebhookPayload payload = new OrderWebhookPayload(90023, resource);

        ResponseEntity<Void> response = controller.created(Map.of("x-api-key", "invalid-key"), payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    private Map<String, String> headers() {
        return Map.of("x-api-key", "valid-key");
    }
}
