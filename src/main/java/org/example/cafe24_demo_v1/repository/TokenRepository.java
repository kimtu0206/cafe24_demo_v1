package org.example.cafe24_demo_v1.repository;
import org.example.cafe24_demo_v1.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository
        extends JpaRepository<Token, Long> {

    Optional<Token> findByMallIdAndClientId(
            String mallId,
            String clientId
    );

}