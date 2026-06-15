package org.example.cafe24_demo_v1.repository;
import org.example.cafe24_demo_v1.entity.Cafe24Token;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface Cafe24TokenRepository
        extends JpaRepository<Cafe24Token, Long> {

    Optional<Cafe24Token> findByMallId(String mallId);
}