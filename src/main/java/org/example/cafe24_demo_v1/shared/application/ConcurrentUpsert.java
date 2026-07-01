package org.example.cafe24_demo_v1.shared.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * (mall_id, 리소스 식별자) unique 제약이 있는 Aggregate를 동시에 반영하는 여러 경로(주기 동기화/Webhook 등)가
 * 경쟁하면 INSERT가 DataIntegrityViolationException으로 실패할 수 있다. 이 경우 다른 트랜잭션이 먼저 넣은
 * 행을 재조회해 갱신으로 폴백한다. Order/Product/Carrier/Benefit 서비스에서 동일하게 반복되던 패턴을 통합한다.
 */
@Slf4j
public final class ConcurrentUpsert {

    private ConcurrentUpsert() {}

    public static <T> void apply(String context, Object logKey, Supplier<Optional<T>> finder,
                                  Consumer<T> onExisting, Runnable onNew) {
        try {
            finder.get().ifPresentOrElse(onExisting, onNew);
        } catch (DataIntegrityViolationException e) {
            log.info("{} 동시 삽입 경쟁으로 충돌, 재조회 후 갱신으로 폴백: {}", context, logKey);
            finder.get().ifPresent(onExisting);
        }
    }
}
