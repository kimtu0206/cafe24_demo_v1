package org.example.cafe24_demo_v1.benefit.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24BenefitListResponse {
    private List<Cafe24BenefitPayload> benefits;
}
