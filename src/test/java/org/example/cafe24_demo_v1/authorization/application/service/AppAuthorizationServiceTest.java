package org.example.cafe24_demo_v1.authorization.application.service;

import org.example.cafe24_demo_v1.authorization.domain.model.AppAuthorization;
import org.example.cafe24_demo_v1.authorization.domain.model.AuthorizationId;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.authorization.domain.repository.AppAuthorizationRepository;
import org.example.cafe24_demo_v1.authorization.domain.service.Cafe24OAuthPort;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppAuthorizationServiceTest {

    @Mock private AppAuthorizationRepository repository;
    @Mock private Cafe24OAuthPort oAuthPort;

    private AppAuthorizationService service;

    @BeforeEach
    void setUp() {
        Cafe24Properties properties = new Cafe24Properties();
        properties.setClientId("client-id");
        service = new AppAuthorizationService(repository, oAuthPort, properties);
    }

    @Test
    void 액세스_토큰이_유효하면_refresh_없이_그대로_반환한다() {
        TokenCredential validCredential = new TokenCredential(
                "access-token", "refresh-token", "Bearer", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
        );
        AppAuthorization authorization = AppAuthorization.grant(new AuthorizationId("mymall", "client-id"), validCredential);
        given(repository.findById(new AuthorizationId("mymall", "client-id"))).willReturn(Optional.of(authorization));

        TokenCredential result = service.getValidCredential("mymall");

        assertThat(result).isEqualTo(validCredential);
        verify(oAuthPort, never()).refreshToken(ArgumentMatchers.any());
        verify(repository, never()).save(authorization);
    }

    @Test
    void 액세스_토큰이_만료됐으면_자동으로_refresh한다() {
        TokenCredential expiredCredential = new TokenCredential(
                "old-access-token", "refresh-token", "Bearer", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(1)
        );
        AppAuthorization authorization = AppAuthorization.grant(new AuthorizationId("mymall", "client-id"), expiredCredential);
        given(repository.findById(new AuthorizationId("mymall", "client-id"))).willReturn(Optional.of(authorization));

        TokenCredential refreshedCredential = new TokenCredential(
                "new-access-token", "refresh-token", "Bearer", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
        );
        given(oAuthPort.refreshToken("refresh-token")).willReturn(refreshedCredential);

        TokenCredential result = service.getValidCredential("mymall");

        assertThat(result).isEqualTo(refreshedCredential);
        verify(repository).save(authorization);
    }
}
