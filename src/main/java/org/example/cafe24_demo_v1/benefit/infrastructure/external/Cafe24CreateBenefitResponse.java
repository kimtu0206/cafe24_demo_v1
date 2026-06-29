package org.example.cafe24_demo_v1.benefit.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24CreateBenefitResponse {
    private Cafe24BenefitPayload benefit;
}
