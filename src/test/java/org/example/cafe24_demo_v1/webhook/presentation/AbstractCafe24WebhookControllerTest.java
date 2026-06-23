package org.example.cafe24_demo_v1.webhook.presentation;

import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AbstractCafe24WebhookControllerTest {

    private final AbstractCafe24WebhookController controller =
            new AppUninstallWebhookController(mock(Cafe24WebhookVerifier.class), mock(ApplicationEventPublisher.class));

    @Test
    void 중복_Webhook으로_인한_unique_제약_위반은_200으로_응답한다() {
        ResponseEntity<Void> response = controller.handleDuplicateWebhook(new DuplicateKeyException("duplicate"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
