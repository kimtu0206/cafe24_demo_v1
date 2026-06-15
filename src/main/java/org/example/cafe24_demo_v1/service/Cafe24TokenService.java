package org.example.cafe24_demo_v1.service;

import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.config.Cafe24Properties;
import org.example.cafe24_demo_v1.entity.Cafe24Token;
import org.example.cafe24_demo_v1.dto.Cafe24TokenResponse;
import org.example.cafe24_demo_v1.repository.Cafe24TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class Cafe24TokenService {

    private final Cafe24TokenRepository repository;
    private final Cafe24Properties cafe24Properties;

    @Transactional
    public void saveToken(
            String mallId,
            Cafe24TokenResponse response
    ) {

        Cafe24Token token =
                repository.findByMallId(mallId)
                        .orElse(new Cafe24Token());

        token.setMallId(mallId);

        token.setAccessToken(
                response.getAccessToken()
        );

        token.setRefreshToken(
                response.getRefreshToken()
        );

        token.setTokenType(
                response.getTokenType()
        );

        token.setExpiresAt(
                LocalDateTime.parse(
                        response.getExpiresAt()
                )
        );

        token.setUpdatedAt(
                LocalDateTime.now()
        );

        if (token.getCreatedAt() == null) {
            token.setCreatedAt(
                    LocalDateTime.now()
            );
        }

        repository.save(token);
    }
}
