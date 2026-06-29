package org.example.cafe24_demo_v1.customergroup.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.customergroup.domain.model.CustomerGroup;
import org.example.cafe24_demo_v1.customergroup.domain.service.Cafe24CustomerGroupPort;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class Cafe24CustomerGroupClient implements Cafe24CustomerGroupPort {

    private final Cafe24Properties properties;
    private final RestTemplate restTemplate;

    public Cafe24CustomerGroupClient(Cafe24Properties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<CustomerGroup> listCustomerGroups(String mallId, TokenCredential credential) {
        String url = "https://" + mallId + ".cafe24api.com/api/v2/admin/customergroups";

        Cafe24CustomerGroupListResponse response = exchange(mallId, url, credential);

        List<Cafe24CustomerGroupPayload> groups = response.getCustomergroups();
        if (groups == null || groups.isEmpty()) {
            return Collections.emptyList();
        }
        return groups.stream().map(this::toDomain).toList();
    }

    private Cafe24CustomerGroupListResponse exchange(String mallId, String url, TokenCredential credential) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(credential.getAccessToken());
        headers.set("X-Cafe24-Api-Version", properties.getApiVersion());

        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
                    Cafe24CustomerGroupListResponse.class).getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Cafe24 customergroups API call failed: mallId={}, status={}, body={}",
                    mallId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new Cafe24ApiException(
                    "Cafe24 customergroups API call failed. status=" + e.getStatusCode(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private CustomerGroup toDomain(Cafe24CustomerGroupPayload payload) {
        return new CustomerGroup(
                payload.getGroupNo(),
                payload.getGroupName(),
                payload.getGroupDescription(),
                payload.getIsBuyer(),
                payload.getIsPrimary()
        );
    }
}
