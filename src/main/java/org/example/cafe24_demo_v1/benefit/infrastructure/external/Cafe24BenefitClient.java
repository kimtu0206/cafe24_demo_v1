package org.example.cafe24_demo_v1.benefit.infrastructure.external;

import lombok.extern.slf4j.Slf4j;
import org.example.cafe24_demo_v1.authorization.domain.model.TokenCredential;
import org.example.cafe24_demo_v1.benefit.domain.model.Benefit;
import org.example.cafe24_demo_v1.benefit.domain.service.Cafe24BenefitPort;
import org.example.cafe24_demo_v1.shared.config.Cafe24Properties;
import org.example.cafe24_demo_v1.shared.exception.Cafe24ApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class Cafe24BenefitClient implements Cafe24BenefitPort {

    private final Cafe24Properties properties;
    private final RestTemplate restTemplate;

    public Cafe24BenefitClient(Cafe24Properties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Benefit> listBenefits(String mallId, String useBenefit, String startDate, String endDate, TokenCredential credential) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(baseUrl(mallId) + "/benefits");

        if (StringUtils.hasText(useBenefit)) {
            builder.queryParam("use_benefit", useBenefit);
        }
        if (StringUtils.hasText(startDate)) {
            builder.queryParam("benefit_start_date", startDate);
        }
        if (StringUtils.hasText(endDate)) {
            builder.queryParam("benefit_end_date", endDate);
        }

        String url = builder.toUriString();

        Cafe24BenefitListResponse response = exchange(
                mallId, url, HttpMethod.GET, new HttpEntity<>(headers(credential)), Cafe24BenefitListResponse.class
        );

        List<Cafe24BenefitPayload> benefits = response.getBenefits();
        if (benefits == null || benefits.isEmpty()) {
            return Collections.emptyList();
        }

        return benefits.stream().map(this::toDomain).toList();
    }

    private String baseUrl(String mallId) {
        return "https://" + mallId + ".cafe24api.com/api/v2/admin";
    }

    private HttpHeaders headers(TokenCredential credential) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(credential.getAccessToken());
        headers.set("X-Cafe24-Api-Version", properties.getApiVersion());
        return headers;
    }

    private <T> T exchange(String mallId, String url, HttpMethod method, HttpEntity<?> request, Class<T> responseType) {
        try {
            return restTemplate.exchange(url, method, request, responseType).getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Cafe24 benefit API call failed: mallId={}, status={}", mallId, e.getStatusCode());
            log.debug("Cafe24 benefit API error body: {}", e.getResponseBodyAsString());
            throw new Cafe24ApiException(
                    "Cafe24 benefit API call failed. status=" + e.getStatusCode(),
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );
        }
    }

    private Benefit toDomain(Cafe24BenefitPayload payload) {
        return new Benefit(
                payload.getShopNo(),
                payload.getBenefitNo(),
                payload.getUseBenefit(),
                payload.getBenefitName(),
                payload.getBenefitDivision(),
                payload.getBenefitType(),
                payload.getUseBenefitPeriod(),
                payload.getBenefitStartDate() != null ? payload.getBenefitStartDate().toLocalDateTime() : null,
                payload.getBenefitEndDate() != null ? payload.getBenefitEndDate().toLocalDateTime() : null,
                payload.getPlatformTypes(),
                payload.getUseGroupBinding(),
                payload.getCustomerGroupList(),
                payload.getProductBindingType(),
                payload.getUseExceptCategory(),
                payload.getIconUrl(),
                payload.getAvailableCoupon(),
                payload.getRepurchaseSale(),
                payload.getBulkPurchaseSale(),
                payload.getMemberSale()
        );
    }
}
