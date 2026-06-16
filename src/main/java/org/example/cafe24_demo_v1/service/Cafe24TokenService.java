package org.example.cafe24_demo_v1.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.entity.Cafe24Token;
import org.example.cafe24_demo_v1.dto.Cafe24TokenResponse;
import org.example.cafe24_demo_v1.repository.Cafe24TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class Cafe24TokenService {

    private final Cafe24TokenRepository tokenRepository;
    private final Cafe24OAuthService oauthService;
    private final Cafe24Properties cafe24Properties;

    @Transactional
    public void saveToken(String mallId, Cafe24TokenResponse response) {
        Cafe24Token token = tokenRepository
                .findByMallIdAndClientId(mallId, cafe24Properties.getClientId())
                .orElse(new Cafe24Token());

        token.setMallId(mallId);
        token.setClientId(cafe24Properties.getClientId());
        token.setAccessToken(response.getAccessToken());
        token.setRefreshToken(response.getRefreshToken());
        token.setTokenType(response.getTokenType());
        token.setAccessTokenExpiresAt(parseCafe24DateTime(response.getExpiresAt()));
        token.setRefreshTokenExpiresAt(parseCafe24DateTime(response.getRefreshTokenExpiresAt()));

        tokenRepository.save(token);
    }

    @Transactional
    public void refreshToken(String mallId) {
        Cafe24Token token = tokenRepository
                .findByMallIdAndClientId(mallId, cafe24Properties.getClientId())
                .orElseThrow();

        Cafe24TokenResponse refreshed = oauthService.refreshAccessToken(token.getRefreshToken());
        saveToken(mallId, refreshed);
    }

    private LocalDateTime parseCafe24DateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.parse(value).toLocalDateTime();
        }
    }
}
