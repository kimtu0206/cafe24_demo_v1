package org.example.cafe24_demo_v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cafe24WebhookRequest {

    @JsonProperty("event_no")
    private Integer eventNo;

    private Resource resource;

    @Getter
    @Setter
    public static class Resource {

        @JsonProperty("mall_id")
        private String mallId;

        @JsonProperty("client_id")
        private String clientId;

        @JsonProperty("app_name")
        private String appName;

        @JsonProperty("deleted_date")
        private String deletedDate;
    }
}
