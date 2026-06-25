package org.example.cafe24_demo_v1.carrier.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.application.command.RegisterCarrierCommand;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.model.ShippingType;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierRepository;
import org.example.cafe24_demo_v1.carrier.domain.service.Cafe24CarrierPort;
import org.example.cafe24_demo_v1.monitoring.application.service.SyncMetricsService;
import org.example.cafe24_demo_v1.monitoring.domain.model.SyncTarget;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CarrierServiceTest {

    @Mock private CarrierRepository repository;
    @Mock private Cafe24CarrierPort cafe24CarrierPort;
    @Mock private AppAuthorizationService authorizationService;
    @Mock private SyncMetricsService syncMetricsService;

    private CarrierService carrierService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        carrierService = new CarrierService(repository, cafe24CarrierPort, authorizationService, syncMetricsService);
    }

    @Test
    void syncFromCafe24는_페이지가_가득_찰_때까지_반복_조회한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        List<Carrier> fullPage = fixedSizeCarriers(100, 1);
        List<Carrier> lastPage = fixedSizeCarriers(20, 101);

        given(cafe24CarrierPort.getCarriers("mymall", 0, 100, credential)).willReturn(fullPage);
        given(cafe24CarrierPort.getCarriers("mymall", 100, 100, credential)).willReturn(lastPage);
        given(repository.findByMallIdAndShippingCarrierCode(any(), any())).willReturn(Optional.empty());

        carrierService.syncFromCafe24("mymall");

        verify(cafe24CarrierPort).getCarriers("mymall", 0, 100, credential);
        verify(cafe24CarrierPort).getCarriers("mymall", 100, 100, credential);
        verify(repository, times(120)).save(any());
    }

    @Test
    void syncFromCafe24는_한_건_처리가_실패해도_나머지_건을_계속_처리한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        Carrier failing = Carrier.register(
                "mymall", null, "fail-1", "실패배송사", null, null, null, null, null, null,
                ShippingType.DOMESTIC, false, false
        );
        Carrier succeeding = Carrier.register(
                "mymall", null, "ok-1", "성공배송사", null, null, null, null, null, null,
                ShippingType.DOMESTIC, false, false
        );
        given(cafe24CarrierPort.getCarriers("mymall", 0, 100, credential)).willReturn(List.of(failing, succeeding));
        given(repository.findByMallIdAndShippingCarrierCode(any(), any())).willReturn(Optional.empty());
        willThrow(new RuntimeException("DB 순단")).given(repository).save(failing);

        carrierService.syncFromCafe24("mymall");

        verify(repository).save(succeeding);
        verify(syncMetricsService).recordRun("mymall", SyncTarget.CARRIER, 1, 1, 0, null);
    }

    @Test
    void syncFromCafe24는_Cafe24_API_호출이_실패하면_이번_실행만_중단한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Cafe24ApiException apiException = new Cafe24ApiException(
                "Cafe24 carrier API call failed. status=500", HttpStatus.INTERNAL_SERVER_ERROR, "{}", null);
        willThrow(apiException).given(cafe24CarrierPort).getCarriers("mymall", 0, 100, credential);

        carrierService.syncFromCafe24("mymall");

        verify(syncMetricsService).recordRun(
                "mymall", SyncTarget.CARRIER, 0, 0, 1, apiException.getMessage());
    }

    @Test
    void syncFromCafe24는_인증_실패하면_이번_실행만_중단하고_API_실패로_기록한다() {
        IllegalStateException authException = new IllegalStateException("Authorization not found: mymall");
        willThrow(authException).given(authorizationService).getValidCredential("mymall");

        CarrierService.SyncResult result = carrierService.syncFromCafe24("mymall");

        assertThat(result.apiFailureCount()).isEqualTo(1);
        assertThat(result.processedCount()).isZero();
        verify(cafe24CarrierPort, never()).getCarriers(any(), anyInt(), anyInt(), any());
        verify(syncMetricsService).recordRun(
                "mymall", SyncTarget.CARRIER, 0, 0, 1, authException.getMessage());
    }

    @Test
    void syncFromCafe24는_동시_삽입_경쟁으로_충돌하면_재조회후_갱신으로_폴백한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        Carrier snapshot = Carrier.register(
                "mymall", null, "0022", "변경된 배송사명", null, null, null, null, null, null,
                ShippingType.DOMESTIC, false, false
        );
        given(cafe24CarrierPort.getCarriers("mymall", 0, 100, credential)).willReturn(List.of(snapshot));

        Carrier concurrentlyInserted = Carrier.register(
                "mymall", 1L, "0022", "기존 배송사명", null, null, null, null, null, null,
                ShippingType.DOMESTIC, false, false
        );
        given(repository.findByMallIdAndShippingCarrierCode("mymall", "0022"))
                .willReturn(Optional.empty(), Optional.of(concurrentlyInserted));
        willThrow(new DataIntegrityViolationException("duplicate entry")).given(repository).save(snapshot);

        carrierService.syncFromCafe24("mymall");

        assertThat(concurrentlyInserted.getShippingCarrierName()).isEqualTo("변경된 배송사명");
        verify(repository).save(concurrentlyInserted);
    }

    @Test
    void registerCarrier는_Cafe24에_등록_후_로컬_DB에_저장한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);

        BigDecimal defaultShippingFee = BigDecimal.valueOf(3000);
        Carrier created = Carrier.register(
                "mymall", 4L, "0022", "FASTBOX", "02-0000-0000", "02-0000-0000",
                "sample@sample.com", null, defaultShippingFee, "sample.sample.com",
                ShippingType.NOT_SET, false, false
        );
        given(cafe24CarrierPort.createCarrier(
                "mymall", "0022", "02-0000-0000", "02-0000-0000", "sample@sample.com",
                defaultShippingFee, "sample.sample.com", null, credential
        )).willReturn(created);

        RegisterCarrierCommand command = new RegisterCarrierCommand(
                "mymall", "0022", "02-0000-0000", "02-0000-0000",
                "sample@sample.com", defaultShippingFee, "sample.sample.com", null
        );

        Carrier result = carrierService.registerCarrier(command);

        assertThat(result).isSameAs(created);
        verify(repository).save(created);
    }

    @Test
    void registerCarrier는_defaultShippingFee가_없으면_Cafe24_호출_없이_예외를_던진다() {
        RegisterCarrierCommand command = new RegisterCarrierCommand(
                "mymall", "0022", "02-0000-0000", "02-0000-0000",
                "sample@sample.com", null, "sample.sample.com", null
        );

        assertThatThrownBy(() -> carrierService.registerCarrier(command))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(authorizationService, cafe24CarrierPort, repository, syncMetricsService);
    }

    private List<Carrier> fixedSizeCarriers(int size, int startCode) {
        List<Carrier> carriers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            carriers.add(Carrier.register(
                    "mymall", null, "code" + (startCode + i), "배송사" + i, null, null, null, null, null, null,
                    ShippingType.DOMESTIC, false, false
            ));
        }
        return carriers;
    }
}
