package org.example.cafe24_demo_v1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.dto.Cafe24TokenResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class Cafe24OAuthService {

    private final Cafe24Properties cafe24Properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public Cafe24TokenResponse getAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", cafe24Properties.getRedirectUri());

        Cafe24TokenResponse response = requestToken(body);
        log.info("TOKEN RESPONSE = {}", response);
        return response;
    }

    public Cafe24TokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        return requestToken(body);
    }

    private Cafe24TokenResponse requestToken(MultiValueMap<String, String> body) {
        String tokenUrl = "https://" + cafe24Properties.getMallId() + ".cafe24.com/api/v2/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(cafe24Properties.getClientId(), cafe24Properties.getClientSecret());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            return restTemplate.postForEntity(tokenUrl, request, Cafe24TokenResponse.class).getBody();
        } catch (HttpClientErrorException e) {
            throw new IllegalStateException("Cafe24 token request failed. status=" + e.getStatusCode(), e);
        }
    }
}
