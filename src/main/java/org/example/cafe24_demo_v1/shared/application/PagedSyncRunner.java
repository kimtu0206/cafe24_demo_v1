package org.example.cafe24_demo_v1.shared.application;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Cafe24 목록 API를 offset/limit로 페이지 단위 반복 조회하며 각 항목을 반영하는 동기화 루프를 통합한다.
 * 페이지 조회(pageFetcher) 자체가 실패하면 더 이상 다음 페이지를 시도하지 않고 이번 실행만 안전하게
 * 종료한다 — 누락된 나머지는 다음 스케줄 실행이 보완한다. 개별 항목 처리 실패는 건너뛰고 계속 진행한다.
 * Order/Product/Carrier 서비스에서 동일하게 반복되던 패턴을 통합한다.
 */
@Slf4j
public final class PagedSyncRunner {

    private PagedSyncRunner() {}

    public static <T> SyncResult run(String context, String mallId, int pageSize,
                                      BiFunction<Integer, Integer, List<T>> pageFetcher,
                                      Consumer<T> itemProcessor, Function<T, Object> idExtractor) {
        int offset = 0;
        int processedCount = 0;
        int failedCount = 0;
        List<T> page;
        while (true) {
            try {
                page = pageFetcher.apply(offset, pageSize);
            } catch (Cafe24ApiException e) {
                log.error("{} sync Cafe24 API 호출 실패, 이번 실행 중단: mallId={}, offset={}", context, mallId, offset, e);
                return new SyncResult(processedCount, failedCount, 1, e.getMessage());
            }
            for (T item : page) {
                try {
                    itemProcessor.accept(item);
                    processedCount++;
                } catch (Exception e) {
                    log.error("{} sync 중 1건 실패, 다음 건 계속 진행: mallId={}, id={}", context, mallId, idExtractor.apply(item), e);
                    failedCount++;
                }
            }
            offset += pageSize;
            if (page.size() < pageSize) {
                break;
            }
        }
        return new SyncResult(processedCount, failedCount, 0, null);
    }
}
