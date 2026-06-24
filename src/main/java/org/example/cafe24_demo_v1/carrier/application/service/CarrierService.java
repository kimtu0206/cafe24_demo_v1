package org.example.cafe24_demo_v1.carrier.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.application.command.RegisterCarrierCommand;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierRepository;
import org.example.cafe24_demo_v1.carrier.domain.service.Cafe24CarrierPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Cafe24 배송사 전체를 페이지 단위로 조회해 로컬 DB와 동기화한다.
     * Webhook 수신 여부와 무관하게 독립적으로 동작하는 안전망 역할이다(CarrierSyncScheduler가 주기 호출).
     * Cafe24 배송사 목록 API는 변경 시점 필터를 제공하지 않으므로, 매번 전체를 다시 조회한다.
     */
    @Transactional
    public void syncFromCafe24(String mallId) {
        TokenCredential credential = authorizationService.getValidCredential(mallId);

        int offset = 0;
        int syncedCount = 0;
        List<Carrier> page;
        do {
            page = cafe24CarrierPort.getCarriers(mallId, offset, SYNC_PAGE_SIZE, credential);
            page.forEach(this::upsert);
            syncedCount += page.size();
            offset += SYNC_PAGE_SIZE;
        } while (page.size() == SYNC_PAGE_SIZE);

        log.info("Carrier sync finished: mallId={}, syncedCount={}", mallId, syncedCount);
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

    /** Cafe24 스냅샷을 로컬 DB에 반영한다. 이미 있으면 갱신, 없으면 신규 저장(Upsert). */
    private void upsert(Carrier snapshot) {
        repository.findByMallIdAndShippingCarrierCode(snapshot.getMallId(), snapshot.getShippingCarrierCode())
                .ifPresentOrElse(
                        existing -> {
                            existing.applySnapshot(
                                    snapshot.getShippingCarrierName(), snapshot.getContact(), snapshot.getSecondaryContact(),
                                    snapshot.getEmail(), snapshot.getTrackShipmentUrl(), snapshot.getDefaultShippingFee(),
                                    snapshot.getHomepageUrl(), snapshot.getShippingType(), snapshot.isDefaultCarrier(),
                                    snapshot.isShippingFeeSetting()
                            );
                            repository.save(existing);
                        },
                        () -> repository.save(snapshot)
                );
    }
}
