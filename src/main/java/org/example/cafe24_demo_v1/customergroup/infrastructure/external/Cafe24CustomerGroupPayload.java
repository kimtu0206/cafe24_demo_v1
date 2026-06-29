package org.example.cafe24_demo_v1.customergroup.infrastructure.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Cafe24CustomerGroupPayload {

    @JsonProperty("group_no")
    private Integer groupNo;

    @JsonProperty("group_name")
    private String groupName;

    @JsonProperty("group_description")
    private String groupDescription;

    @JsonProperty("is_buyer")
    private String isBuyer;

    @JsonProperty("is_primary")
    private String isPrimary;
}
