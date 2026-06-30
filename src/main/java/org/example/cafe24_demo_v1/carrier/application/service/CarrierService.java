package org.example.cafe24_demo_v1.carrier.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.application.command.RegisterCarrierCommand;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierRepository;
import org.example.cafe24_demo_v1.carrier.domain.service.Cafe24CarrierPort;
import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.shared.application.SyncResult;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 배송사(Carrier) 관련 유즈케이스를 조율하는 애플리케이션 서비스.
 *
 * Cafe24 API 호출에 필요한 액세스 토큰은 authorization 컨텍스트(AppAuthorizationService)에서
 * 가져온다. carrier → authorization 단방향 의존이며, product → authorization 의존과 같은 패턴이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierService {

    private static final int SYNC_PAGE_SIZE = 100;

    private final CarrierRepository repository;
    private final Cafe24CarrierPort cafe24CarrierPort;
    private final AppAuthorizationService authorizationService;
    private final SyncMetricsService syncMetricsService;

    /**
     * Cafe24 배송사 전체를 페이지 단위로 조회해 로컬 DB와 동기화한다.
     * Webhook 수신 여부와 무관하게 독립적으로 동작하는 안전망 역할이다(CarrierSyncScheduler가 주기 호출).
     * Cafe24 배송사 목록 API는 변경 시점 필터를 제공하지 않으므로, 매번 전체를 다시 조회한다.
     *
     * Cafe24 호출(외부 I/O)에는 트랜잭션을 걸지 않는다 — DB 커넥션을 외부 응답 대기 시간만큼
     * 점유하지 않기 위함이다(Order와 동일한 이유). 한 건의 upsert가 실패해도 나머지 건은 계속
     * 처리한다 — 한 건의 실패가 같은 배치의 다른 배송사 반영까지 막아서는 안 되기 때문이다.
     */
    public SyncResult syncFromCafe24(String mallId) {
        TokenCredential credential;
        try {
            credential = authorizationService.getValidCredential(mallId);
        } catch (Exception e) {
            log.error("Carrier sync 인증 실패, 이번 실행 중단: mallId={}", mallId, e);
            SyncResult failure = new SyncResult(0, 0, 1, e.getMessage());
            syncMetricsService.recordRun(mallId, SyncTarget.CARRIER,
                    failure.processedCount(), failure.failedCount(), failure.apiFailureCount(), failure.errorMessage());
            return failure;
        }

        SyncResult result = syncPages(mallId, credential);
        syncMetricsService.recordRun(mallId, SyncTarget.CARRIER,
                result.processedCount(), result.failedCount(), result.apiFailureCount(), result.errorMessage());

        log.info("Carrier sync finished: mallId={}, processedCount={}, failedCount={}, apiFailureCount={}",
                mallId, result.processedCount(), result.failedCount(), result.apiFailureCount());
        return result;
    }

    /**
     * Cafe24 API 호출(getCarriers) 자체가 실패하면 더 이상 다음 페이지를 시도하지 않고 이번
     * 실행만 안전하게 종료한다 — 누락된 나머지는 다음 스케줄 실행이 보완한다.
     */
    private SyncResult syncPages(String mallId, TokenCredential credential) {
        int offset = 0;
        int processedCount = 0;
        int failedCount = 0;
        List<Carrier> page;
        while (true) {
            try {
                page = cafe24CarrierPort.getCarriers(mallId, offset, SYNC_PAGE_SIZE, credential);
            } catch (Cafe24ApiException e) {
                log.error("Carrier sync Cafe24 API 호출 실패, 이번 실행 중단: mallId={}, offset={}", mallId, offset, e);
                return new SyncResult(processedCount, failedCount, 1, e.getMessage());
            }
            for (Carrier snapshot : page) {
                try {
                    upsert(snapshot);
                    processedCount++;
                } catch (Exception e) {
                    log.error("Carrier sync 중 1건 실패, 다음 건 계속 진행: mallId={}, shippingCarrierCode={}",
                            mallId, snapshot.getShippingCarrierCode(), e);
                    failedCount++;
                }
            }
            offset += SYNC_PAGE_SIZE;
            if (page.size() < SYNC_PAGE_SIZE) {
                break;
            }
        }

        return new SyncResult(processedCount, failedCount, 0, null);
    }

    /**
     * Cafe24에 새 배송사를 등록하고 결과를 로컬 DB에 저장한다.
     * Cafe24 응답 대기 시간만큼 DB 커넥션을 점유하지 않도록 @Transactional을 두지 않는다(외부 호출 후 단건 저장만 수행).
     */
    public Carrier registerCarrier(RegisterCarrierCommand command) {
        // 배송비 상세 설정을 항상 미설정("F")으로 보내므로, Cafe24는 이 경우 default_shipping_fee가 반드시 있어야 한다고 요구한다.
        if (command.defaultShippingFee() == null) {
            throw new IllegalArgumentException("defaultShippingFee는 필수입니다. 배송비 상세 설정을 사용하지 않으므로 기본 배송비가 반드시 필요합니다.");
        }

        TokenCredential credential = authorizationService.getValidCredential(command.mallId());

        Carrier created = cafe24CarrierPort.createCarrier(
                command.mallId(),
                command.shippingCarrierCode(),
                command.contact(),
                command.secondaryContact(),
                command.email(),
                command.defaultShippingFee(),
                command.homepageUrl(),
                command.trackShipmentUrl(),
                credential
        );

        repository.save(created);
        log.info("Carrier registered: mallId={}, shippingCarrierCode={}", command.mallId(), created.getShippingCarrierCode());
        return created;
    }

    /**
     * Cafe24 스냅샷을 로컬 DB에 반영한다. 이미 있으면 갱신, 없으면 신규 저장(Upsert).
     *
     * (mall_id, shipping_carrier_code) unique 제약 때문에, 같은 배송사를 동시에 반영하는 다른
     * 경로와 경쟁하면 INSERT가 DataIntegrityViolationException으로 실패할 수 있다. 이 경우 다른
     * 트랜잭션이 먼저 넣은 행을 재조회해 갱신으로 폴백한다(Order.upsert와 동일한 패턴). 이 메서드의
     * 유일한 호출부인 syncFromCafe24는 @Transactional이 없어 save() 호출마다 독립된 트랜잭션으로
     * 즉시 flush되므로 이 폴백이 안전하게 동작한다.
     */
    private void upsert(Carrier snapshot) {
        try {
            findAndApply(snapshot);
        } catch (DataIntegrityViolationException e) {
            log.info("Carrier sync 중 동시 삽입 경쟁으로 충돌, 재조회 후 갱신으로 폴백: mallId={}, shippingCarrierCode={}",
                    snapshot.getMallId(), snapshot.getShippingCarrierCode());
            repository.findByMallIdAndShippingCarrierCode(snapshot.getMallId(), snapshot.getShippingCarrierCode())
                    .ifPresent(existing -> applySnapshotAndSave(existing, snapshot));
        }
    }

    private void findAndApply(Carrier snapshot) {
        repository.findByMallIdAndShippingCarrierCode(snapshot.getMallId(), snapshot.getShippingCarrierCode())
                .ifPresentOrElse(
                        existing -> applySnapshotAndSave(existing, snapshot),
                        () -> repository.save(snapshot)
                );
    }

    private void applySnapshotAndSave(Carrier existing, Carrier snapshot) {
        existing.applySnapshot(
                snapshot.getShippingCarrierName(), snapshot.getContact(), snapshot.getSecondaryContact(),
                snapshot.getEmail(), snapshot.getTrackShipmentUrl(), snapshot.getDefaultShippingFee(),
                snapshot.getHomepageUrl(), snapshot.getShippingType(), snapshot.isDefaultCarrier(),
                snapshot.isShippingFeeSetting()
        );
        repository.save(existing);
    }
}
