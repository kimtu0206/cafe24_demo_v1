package org.example.cafe24_demo_v1.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.dto.TokenResponse;
import org.example.cafe24_demo_v1.service.OAuthService;
import org.example.cafe24_demo_v1.service.TokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController {

    private static final String SESSION_STATE_KEY = "cafe24_oauth_state";

    private final Cafe24Properties cafe24Properties;
    private final OAuthService oauthService;
    private final TokenService tokenService;

    @GetMapping("/oauth/login")
    public void login(HttpServletResponse response, HttpSession session) throws IOException {
        String state = cafe24Properties.getState();
        session.setAttribute(SESSION_STATE_KEY, state);

        String authUrl = UriComponentsBuilder
                .fromUriString("https://" + cafe24Properties.getMallId() + ".cafe24.com/api/v2/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", cafe24Properties.getClientId())
                .queryParam("state", state)
                .queryParam("redirect_uri", cafe24Properties.getRedirectUri())
                .queryParam("scope", cafe24Properties.getScope())
                .build()
                .encode()
                .toUriString();

        log.info("AUTH URL = {}", authUrl);
        response.sendRedirect(authUrl);
    }

    @GetMapping("/oauth/callback")
    public String callback(@RequestParam String code, @RequestParam String state, HttpSession session) {
        String savedState = (String) session.getAttribute(SESSION_STATE_KEY);

        if (savedState == null || !savedState.equals(state)) {
            throw new IllegalArgumentException("Invalid OAuth state");
        }

        session.removeAttribute(SESSION_STATE_KEY);

        TokenResponse token = oauthService.getAccessToken(code);
        tokenService.saveToken(cafe24Properties.getMallId(), token);

        return "success";
    }

    @PostMapping("/oauth/refresh")
    public String refresh() {
        tokenService.refreshToken(cafe24Properties.getMallId());
        return "success";
    }
}
