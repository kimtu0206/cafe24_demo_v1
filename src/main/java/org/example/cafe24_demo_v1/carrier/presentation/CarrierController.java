package org.example.cafe24_demo_v1.carrier.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.cafe24_demo_v1.carrier.application.command.RegisterCarrierCommand;
import org.example.cafe24_demo_v1.carrier.application.service.CarrierService;
import org.example.cafe24_demo_v1.carrier.domain.model.Carrier;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배송사(Carrier) 등록 API를 처리하는 컨트롤러.
 *
 * HTTP 요청/응답 변환만 담당하고, 실제 Cafe24 등록과 로컬 DB 저장은 CarrierService에 위임한다.
 */
@Tag(name = "Carriers", description = "배송사 등록 — Cafe24 API 연동")
@RestController
@RequestMapping("/carriers")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierService carrierService;
    private final Cafe24Properties cafe24Properties;

    @Operation(summary = "배송사 등록", description = "Cafe24에 배송사를 등록하고 로컬 DB에 저장합니다. shippingCarrierCode는 Cafe24에 사전 등록된 배송사 코드여야 합니다.")
    @PostMapping
    public ResponseEntity<CarrierRegisterResponse> register(@RequestBody CarrierRegisterRequest request) {
        RegisterCarrierCommand command = new RegisterCarrierCommand(
                cafe24Properties.getMallId(),
                request.shippingCarrierCode(),
                request.contact(),
                request.secondaryContact(),
                request.email(),
                request.defaultShippingFee(),
                request.homepageUrl(),
                request.trackShipmentUrl()
        );

        Carrier created = carrierService.registerCarrier(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CarrierRegisterResponse.from(created));
    }
}
