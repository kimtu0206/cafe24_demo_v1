package org.example.cafe24_demo_v1.authorization.presentation;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.command.GrantAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.application.command.RefreshAuthorizationCommand;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth 인가 흐름을 처리하는 컨트롤러.
 *
 * HTTP 요청/응답 처리만 담당하고, 비즈니스 로직은 AppAuthorizationService에 위임한다.
 * HTTP 파라미터 → Command 객체 변환 → 서비스 호출의 단순한 흐름을 유지한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController {

    // CSRF 공격 방지를 위해 세션에 state 값을 저장하는 키
    private static final String SESSION_STATE_KEY = "cafe24_oauth_state";

    private final Cafe24Properties cafe24Properties;
    private final AppAuthorizationService authorizationService;

    /**
     * OAuth 로그인 시작 엔드포인트.
     * CSRF 방지용 state 값을 세션에 저장하고, Cafe24 인가 서버로 리다이렉트한다.
     */
    @GetMapping("/oauth/login")
    public void login(HttpServletResponse response, HttpSession session) throws IOException {
        String state = cafe24Properties.getState();
        session.setAttribute(SESSION_STATE_KEY, state); // CSRF 검증을 위해 세션에 보관

        String authUrl = UriComponentsBuilder
                .fromUriString("https://" + cafe24Properties.getMallId() + ".cafe24api.com/api/v2/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", cafe24Properties.getClientId())
                .queryParam("state", state)
                .queryParam("redirect_uri", cafe24Properties.getRedirectUri())
                .queryParam("scope", cafe24Properties.getScope())
                .build()
                .encode()
                .toUriString();

        response.sendRedirect(authUrl);
    }

    /**
     * Cafe24 인가 서버가 호출하는 OAuth 콜백 엔드포인트.
     * state 검증 후 인가 코드를 AppAuthorizationService에 전달해 토큰을 발급받고 저장한다.
     */
    @GetMapping("/oauth/callback")
    public String callback(@RequestParam String code, @RequestParam String state, HttpSession session) {
        // CSRF 방지: 세션에 저장한 state와 콜백으로 받은 state가 일치하는지 검증
        String savedState = (String) session.getAttribute(SESSION_STATE_KEY);
        if (savedState == null || !savedState.equals(state)) {
            throw new IllegalArgumentException("Invalid OAuth state");
        }
        session.removeAttribute(SESSION_STATE_KEY); // 검증 완료 후 세션에서 제거

        authorizationService.grant(new GrantAuthorizationCommand(cafe24Properties.getMallId(), code));
        return "success";
    }

    /**
     * 액세스 토큰 수동 갱신 엔드포인트.
     * 저장된 리프레시 토큰으로 새 액세스 토큰을 재발급받는다.
     */
    @PostMapping("/oauth/refresh")
    public String refresh() {
        authorizationService.refresh(new RefreshAuthorizationCommand(cafe24Properties.getMallId()));
        return "success";
    }
}
