package org.example.cafe24_demo_v1.carrier.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.model.ShippingType;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierRepository;
import org.example.cafe24_demo_v1.carrier.domain.service.Cafe24CarrierPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CarrierServiceTest {

    @Mock private CarrierRepository repository;
    @Mock private Cafe24CarrierPort cafe24CarrierPort;
    @Mock private AppAuthorizationService authorizationService;

    private CarrierService carrierService;

    private final TokenCredential credential = new TokenCredential(
            "access-token", "refresh-token", "Bearer", LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1)
    );

    @BeforeEach
    void setUp() {
        carrierService = new CarrierService(repository, cafe24CarrierPort, authorizationService);
    }

    @Test
    void upsertFromWebhook은_기존_배송사가_있으면_갱신한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Carrier snapshot = Carrier.register(
                "mymall", null, "01", "변경된 이름", null, null, null, null, null, null,
                ShippingType.INTERNATIONAL, false, false
        );
        given(cafe24CarrierPort.getCarrier("mymall", "01", credential)).willReturn(Optional.of(snapshot));

        Carrier existing = Carrier.reconstitute(
                10L, null, "mymall", "01", "기존 이름", null, null, null, null, null, null,
                ShippingType.DOMESTIC, true, true, LocalDateTime.now(), LocalDateTime.now()
        );
        given(repository.findByMallIdAndShippingCarrierCode("mymall", "01")).willReturn(Optional.of(existing));

        carrierService.upsertFromWebhook("mymall", "01");

        assertThat(existing.getShippingCarrierName()).isEqualTo("변경된 이름");
        assertThat(existing.getShippingType()).isEqualTo(ShippingType.INTERNATIONAL);
        verify(repository).save(existing);
    }

    @Test
    void upsertFromWebhook은_없는_배송사면_신규로_저장한다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        Carrier snapshot = Carrier.register(
                "mymall", null, "02", "신규 배송사", null, null, null, null, null, null,
                ShippingType.DOMESTIC, true, true
        );
        given(cafe24CarrierPort.getCarrier("mymall", "02", credential)).willReturn(Optional.of(snapshot));
        given(repository.findByMallIdAndShippingCarrierCode("mymall", "02")).willReturn(Optional.empty());

        carrierService.upsertFromWebhook("mymall", "02");

        verify(repository).save(snapshot);
    }

    @Test
    void upsertFromWebhook은_Cafe24에_배송사가_없으면_아무것도_저장하지_않는다() {
        given(authorizationService.getValidCredential("mymall")).willReturn(credential);
        given(cafe24CarrierPort.getCarrier("mymall", "58", credential)).willReturn(Optional.empty());

        carrierService.upsertFromWebhook("mymall", "58");

        verify(repository, never()).save(any());
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
