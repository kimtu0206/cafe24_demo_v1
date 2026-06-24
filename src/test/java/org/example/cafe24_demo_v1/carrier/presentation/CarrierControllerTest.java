package org.example.cafe24_demo_v1.carrier.presentation;

import org.example.cafe24_demo_v1.carrier.application.command.RegisterCarrierCommand;
import org.example.cafe24_demo_v1.carrier.application.service.CarrierService;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.carrier.domain.model.ShippingType;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CarrierControllerTest {

    @Mock private CarrierService carrierService;

    private CarrierController controller;

    @BeforeEach
    void setUp() {
        Cafe24Properties properties = new Cafe24Properties();
        properties.setMallId("mymall");
        controller = new CarrierController(carrierService, properties);
    }

    @Test
    void register는_Cafe24Properties의_mallId로_커맨드를_만들어_서비스를_호출하고_201을_반환한다() {
        Carrier created = Carrier.register(
                "mymall", 4L, "0022", "FASTBOX", "02-0000-0000", "02-0000-0000",
                "sample@sample.com", null, null, "sample.sample.com",
                ShippingType.NOT_SET, false, false
        );
        given(carrierService.registerCarrier(any(RegisterCarrierCommand.class))).willReturn(created);

        CarrierRegisterRequest request = new CarrierRegisterRequest(
                "0022", "02-0000-0000", "02-0000-0000", "sample@sample.com", null, "sample.sample.com", null
        );

        ResponseEntity<CarrierRegisterResponse> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().carrierId()).isEqualTo(4L);
        assertThat(response.getBody().shippingCarrierName()).isEqualTo("FASTBOX");

        ArgumentCaptor<RegisterCarrierCommand> captor = ArgumentCaptor.forClass(RegisterCarrierCommand.class);
        verify(carrierService).registerCarrier(captor.capture());
        assertThat(captor.getValue().mallId()).isEqualTo("mymall");
        assertThat(captor.getValue().shippingCarrierCode()).isEqualTo("0022");
    }
}
