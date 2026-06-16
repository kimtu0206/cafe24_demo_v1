package org.example.cafe24_demo_v1.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cafe24")
public class Cafe24Properties {

    private String clientId;

    private String clientSecret;

    private String mallId;

    private String redirectUri;

    private String scope;

    private String state;
}