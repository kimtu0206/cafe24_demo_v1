package org.example.cafe24_demo_v1.webhook.infrastructure;

import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Cafe24WebhookVerifierTest {

    private Cafe24Properties cafe24Properties;
    private Cafe24WebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        cafe24Properties = new Cafe24Properties();
        verifier = new Cafe24WebhookVerifier(cafe24Properties);
    }

    @Test
    void API_Key가_일치하면_검증을_통과한다() {
        cafe24Properties.getWebhook().setApiKey("secret-key");

        boolean result = verifier.verify("secret-key");

        assertThat(result).isTrue();
    }

    @Test
    void API_Key가_일치하지_않으면_검증에_실패한다() {
        cafe24Properties.getWebhook().setApiKey("secret-key");

        boolean result = verifier.verify("wrong-key");

        assertThat(result).isFalse();
    }

    @Test
    void 설정된_API_Key가_비어있으면_검증에_실패한다() {
        cafe24Properties.getWebhook().setApiKey("");

        boolean result = verifier.verify("anything");

        assertThat(result).isFalse();
    }

    @Test
    void 설정된_API_Key가_null이면_검증에_실패한다() {
        cafe24Properties.getWebhook().setApiKey(null);

        boolean result = verifier.verify("anything");

        assertThat(result).isFalse();
    }
}
