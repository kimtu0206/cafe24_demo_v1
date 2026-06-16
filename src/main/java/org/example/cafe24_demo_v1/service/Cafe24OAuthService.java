package org.example.cafe24_demo_v1.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.dto.Cafe24TokenResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class Cafe24OAuthService {
    private final Cafe24Properties cafe24Properties;

    public String getAccessToken(String code) {

        String tokenUrl =
                "https://" + cafe24Properties.getMallId()
                        + ".cafe24api.com/api/v2/oauth/token";

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add(
                "redirect_uri",
                cafe24Properties.getRedirectUri()
        );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        headers.setBasicAuth(
                cafe24Properties.getClientId(),
                cafe24Properties.getClientSecret()
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        try {

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            tokenUrl,
                            request,
                            String.class
                    );

            return response.getBody();

        } catch (HttpClientErrorException e) {

            throw new IllegalStateException(
                    "Cafe24 token request failed. status="
                            + e.getStatusCode(),
                    e
            );
        }
    }

    public Cafe24TokenResponse refreshAccessToken(
            String refreshToken
    ) {

        String tokenUrl =
                "https://" + cafe24Properties.getMallId()
                        + ".cafe24api.com/api/v2/oauth/token";

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        headers.setBasicAuth(
                cafe24Properties.getClientId(),
                cafe24Properties.getClientSecret()
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(body, headers);

        try {

            ResponseEntity<Cafe24TokenResponse> response =
                    restTemplate.postForEntity(
                            tokenUrl,
                            request,
                            Cafe24TokenResponse.class
                    );

            return response.getBody();

        } catch (HttpClientErrorException e) {

            throw new IllegalStateException(
                    "Cafe24 refresh token request failed. status="
                            + e.getStatusCode(),
                    e
            );
        }
    }
}