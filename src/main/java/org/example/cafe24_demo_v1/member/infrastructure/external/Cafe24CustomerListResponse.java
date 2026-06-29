package org.example.cafe24_demo_v1.member.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24CustomerListResponse {
    private List<Cafe24CustomerPayload> customers;
}
