package org.example.cafe24_demo_v1.carrier.application.service;

import org.example.cafe24_demo_v1.authorization.application.service.AppAuthorizationService;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.carrier.application.command.RegisterCarrierCommand;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.model.ShippingType;
import org.example.cafe24_demo_v1.carrier.domain.repository.CarrierRepository;
import org.example.cafe24_demo_v1.carrier.domain.service.Cafe24CarrierPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

        verifyNoInteractions(authorizationService, cafe24CarrierPort, repository);
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
