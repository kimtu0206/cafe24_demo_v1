package org.example.cafe24_demo_v1.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.dto.Cafe24TokenResponse;
import org.example.cafe24_demo_v1.service.Cafe24OAuthService;
import org.example.cafe24_demo_v1.service.Cafe24TokenService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OAuthController {

    private final Cafe24Properties cafe24Properties;
    private final Cafe24OAuthService oauthService;
    private final Cafe24TokenService tokenService;

    @GetMapping("/oauth/callback")
    public String callback(@RequestParam String code)
            throws Exception {

        String responseJson =
                oauthService.getAccessToken(code);

        ObjectMapper mapper = new ObjectMapper();

        Cafe24TokenResponse token =
                mapper.readValue(
                        responseJson,
                        Cafe24TokenResponse.class
                );

        tokenService.saveToken(
                cafe24Properties.getMallId(),
                token
        );

        return "success";
    }

    @GetMapping("/oauth/login")
    public void login(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();

        String authUrl =
                "https://" + cafe24Properties.getMallId() + ".cafe24api.com/api/v2/oauth/authorize"
                        + "?response_type=code"
                        + "&client_id=" + cafe24Properties.getClientId()
                        + "&state=" + state
                        + "&redirect_uri=" + cafe24Properties.getRedirectUri()
                        + "&scope=mall.read_application";

        response.sendRedirect(authUrl);
    }
}