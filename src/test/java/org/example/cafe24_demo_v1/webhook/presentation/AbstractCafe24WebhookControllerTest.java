package org.example.cafe24_demo_v1.webhook.presentation;

import org.example.cafe24_demo_v1.webhook.infrastructure.Cafe24WebhookVerifier;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AbstractCafe24WebhookControllerTest {

    private final AbstractCafe24WebhookController controller =
            new AppUninstallWebhookController(mock(Cafe24WebhookVerifier.class), mock(ApplicationEventPublisher.class));

    @Test
    void 중복_Webhook으로_인한_unique_제약_위반은_200으로_응답한다() {
        DataIntegrityViolationException e = wrap(uniqueConstraintViolation());

        ResponseEntity<Void> response = controller.handleDuplicateWebhook(e);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unique_제약_위반이_아닌_무결성_위반은_그대로_전파한다() {
        DataIntegrityViolationException e = wrap(notNullConstraintViolation());

        assertThatThrownBy(() -> controller.handleDuplicateWebhook(e))
                .isSameAs(e);
    }

    private DataIntegrityViolationException wrap(ConstraintViolationException cause) {
        return new DataIntegrityViolationException(cause.getMessage(), cause);
    }

    private ConstraintViolationException uniqueConstraintViolation() {
        return new ConstraintViolationException(
                "duplicate", new SQLException("duplicate"),
                ConstraintViolationException.ConstraintKind.UNIQUE, "uk_event_no_mall_id_resource_id");
    }

    private ConstraintViolationException notNullConstraintViolation() {
        return new ConstraintViolationException(
                "not null", new SQLException("not null"),
                ConstraintViolationException.ConstraintKind.OTHER, null);
    }
}
