package pl.tomaszsieminski.coupon.infrastructure.external.geoip.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.geoip", name = "mode", havingValue = "external")
public class IpWhoIsGeoIpClient {

    private final RestClient restClient;

    public IpWhoIsGeoIpClient(RestClient ipWhoIsRestClient) {
        this.restClient = ipWhoIsRestClient;
    }

    @CircuitBreaker(name = "geoIpProvider", fallbackMethod = "fallbackCountryCode")
    @Retry(name = "geoIpProvider")
    public Optional<String> fetchCountryCode(String ipAddress) {
        GeoIpResponse response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment(ipAddress)
                        .queryParam("fields", "success,country_code,message")
                        .build())
                .retrieve()
                .body(GeoIpResponse.class);

        return Optional.ofNullable(response)
                .filter(GeoIpResponse::success)
                .map(GeoIpResponse::countryCode)
                .filter(countryCode -> !countryCode.isBlank());
    }

    private Optional<String> fallbackCountryCode(String ipAddress, Throwable throwable) {
        return Optional.empty();
    }

    private record GeoIpResponse(
            boolean success, @JsonProperty("country_code") String countryCode, String message) {}
}
