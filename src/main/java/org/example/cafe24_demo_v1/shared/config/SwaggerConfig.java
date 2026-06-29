package org.example.cafe24_demo_v1.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class SwaggerConfig {

    /** Webhook 컨트롤러의 @SecurityRequirement(name = ...) 값과 일치해야 한다. */
    public static final String WEBHOOK_API_KEY = "WebhookApiKey";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cafe24 Demo API")
                        .description("Cafe24 OAuth 인가 · 주문/상품/배송사 동기화 · Webhook 수신 데모 서버")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes(WEBHOOK_API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("x-api-key")
                                .description("Cafe24 Webhook 검증 키 (CAFE24_WEBHOOK_API_KEY 환경변수 값)")));
    }
}
