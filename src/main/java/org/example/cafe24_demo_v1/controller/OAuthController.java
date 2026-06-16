package org.example.cafe24_demo_v1.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.dto.Cafe24TokenResponse;
import org.example.cafe24_demo_v1.service.Cafe24OAuthService;
import org.example.cafe24_demo_v1.service.Cafe24TokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class OAuthController {

    private final Cafe24Properties cafe24Properties;
    private final Cafe24OAuthService oauthService;
    private final Cafe24TokenService tokenService;

    @GetMapping("/oauth/callback")
    public String callback( @RequestParam String code,
                            @RequestParam String state,
                            HttpSession session) {

        String savedState =
                (String) session.getAttribute(
                        "cafe24_oauth_state"
                );

        if (savedState == null || !savedState.equals(state)) {
            throw new IllegalArgumentException(
                    "Invalid OAuth state"
            );
        }

        session.removeAttribute(
                "cafe24_oauth_state"
        );

        Cafe24TokenResponse token =
                oauthService.getAccessToken(code);

        tokenService.saveToken(
                cafe24Properties.getMallId(),
                token
        );

        return "success";
    }

    @GetMapping("/oauth/login")
    public void login(HttpServletResponse response, HttpSession session) throws IOException {
        String state = cafe24Properties.getState();

        session.setAttribute(
                "cafe24_oauth_state",
                state
        );

        String authUrl = UriComponentsBuilder.fromUriString(
                        "https://"
                                + cafe24Properties.getMallId()
                                + ".cafe24.com/api/v2/oauth/authorize"
                )
                .queryParam(
                        "response_type",
                        "code"
                )
                .queryParam(
                        "client_id",
                        cafe24Properties.getClientId()
                )
                .queryParam(
                        "state",
                        state
                )
                .queryParam(
                        "redirect_uri",
                        cafe24Properties.getRedirectUri()
                )
                .queryParam(
                        "scope",
                        cafe24Properties.getScope()
                )
                .build()
                .encode()
                .toUriString();

        System.out.println("AUTH URL = {}"+ authUrl);



        response.sendRedirect(authUrl);
    }

    @PostMapping("/oauth/refresh")
    public String refresh() {

        tokenService.refreshToken(
                cafe24Properties.getMallId()
        );

        return "success";
    }
}