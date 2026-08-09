package pl.tomaszsieminski.coupon.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pl.tomaszsieminski.coupon.application.port.out.GeoIpService;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponEntity;
import pl.tomaszsieminski.coupon.support.FullStackIntegrationTestSupport;
import pl.tomaszsieminski.coupon.web.dto.CouponResponse;
import pl.tomaszsieminski.coupon.web.dto.CreateCouponRequest;
import pl.tomaszsieminski.coupon.web.dto.RedeemCouponRequest;

@Import(CouponControllerIntegrationTest.CapturingGeoIpConfiguration.class)
class CouponControllerIntegrationTest extends FullStackIntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CapturingGeoIpService geoIpService;

    @BeforeEach
    void resetGeoIpService() {
        geoIpService.resolveCountryAs("PL");
    }

    @Test
    @DisplayName("Should create coupon")
    void shouldCreateCoupon() {
        CreateCouponRequest request = new CreateCouponRequest("WELCOME", 5, "pl");

        ResponseEntity<CouponResponse> response =
                restTemplate.postForEntity("/api/v1/coupons", request, CouponResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasPath("/api/v1/coupons/WELCOME");
        assertThat(response.getBody())
                .isEqualTo(new CouponResponse(response.getBody().id(), "WELCOME", 5, 0, "PL"));

        CouponEntity coupon = couponRepository.findByCodeIgnoreCase("welcome").orElseThrow();
        assertThat(coupon.getCode()).isEqualTo("WELCOME");
        assertThat(coupon.getCountryCode()).isEqualTo("PL");
    }

    @Test
    @DisplayName("Should reject invalid create coupon request")
    void shouldRejectInvalidCreateCouponRequest() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest("VALID", 0, "PL");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/coupons", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("title").asText()).isEqualTo("Validation Failed");
        assertThat(body.path("detail").asText()).isEqualTo("Request validation failed");
        assertThat(body.path("errors").get(0).path("field").asText()).isEqualTo("maxUses");
        assertThat(body.path("errors").get(0).path("message").asText())
                .isEqualTo("Maximum uses must be greater than zero");
    }

    @Test
    @DisplayName("Should reject non-ISO country code")
    void shouldRejectNonIsoCountryCode() throws Exception {
        CreateCouponRequest request = new CreateCouponRequest("VALID", 1, "ZZ");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/coupons", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("title").asText()).isEqualTo("Validation Failed");
        assertThat(body.path("errors").findValuesAsText("field")).contains("countryCode");
        assertThat(body.path("errors").findValuesAsText("message"))
                .contains("Country code must be an ISO 3166-1 alpha-2 country code");
    }

    @Test
    @DisplayName("Should reject duplicate coupon code")
    void shouldRejectDuplicateCouponCode() throws Exception {
        givenCoupon("DUPLICATE", 5, "PL");
        CreateCouponRequest request = new CreateCouponRequest("duplicate", 10, "PL");

        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/coupons", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("title").asText()).isEqualTo("Coupon Already Exists");
    }

    @Test
    @DisplayName("Should redeem coupon using first X-Forwarded-For address")
    void shouldRedeemCouponUsingFirstForwardedAddress() {
        givenCoupon("REDEEM", 2, "PL");
        HttpEntity<RedeemCouponRequest> request =
                new HttpEntity<>(new RedeemCouponRequest("user-1"), jsonHeaders("203.0.113.10, 10.0.0.1"));

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/v1/coupons/REDEEM/redeem", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(geoIpService.lastResolvedIp()).isEqualTo("203.0.113.10");
        CouponEntity coupon = couponRepository.findByCodeIgnoreCase("redeem").orElseThrow();
        assertThat(coupon.getCurrentUses()).isEqualTo(1);
        assertThat(redemptionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject redemption when client country does not match coupon country")
    void shouldRejectRedemptionWhenClientCountryDoesNotMatchCouponCountry() throws Exception {
        givenCoupon("COUNTRY", 2, "PL");
        geoIpService.resolveCountryAs("DE");
        HttpEntity<RedeemCouponRequest> request =
                new HttpEntity<>(new RedeemCouponRequest("user-1"), jsonHeaders(CLIENT_IP));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/coupons/COUNTRY/redeem", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("title").asText()).isEqualTo("Coupon Country Mismatch");
        assertThat(redemptionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should reject redemption when GeoIP country cannot be resolved")
    void shouldRejectRedemptionWhenGeoIpCountryCannotBeResolved() throws Exception {
        givenCoupon("UNKNOWN_COUNTRY", 2, "PL");
        geoIpService.resolveCountryAs(null);
        HttpEntity<RedeemCouponRequest> request =
                new HttpEntity<>(new RedeemCouponRequest("user-1"), jsonHeaders(CLIENT_IP));

        ResponseEntity<String> response =
                restTemplate.postForEntity("/api/v1/coupons/UNKNOWN_COUNTRY/redeem", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.path("title").asText()).isEqualTo("GeoIP Country Resolution Failed");
        assertThat(redemptionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Should expose business redemption metrics in Prometheus format")
    void shouldExposeBusinessRedemptionMetricsInPrometheusFormat() {
        givenCoupon("METRICS", 2, "PL");
        HttpEntity<RedeemCouponRequest> firstRequest =
                new HttpEntity<>(new RedeemCouponRequest("user-1"), jsonHeaders(CLIENT_IP));
        HttpEntity<RedeemCouponRequest> secondRequest =
                new HttpEntity<>(new RedeemCouponRequest("user-1"), jsonHeaders(CLIENT_IP));

        restTemplate.postForEntity("/api/v1/coupons/METRICS/redeem", firstRequest, Void.class);
        restTemplate.postForEntity("/api/v1/coupons/METRICS/redeem", secondRequest, String.class);

        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("coupon_redemptions_total");
        assertThat(response.getBody()).contains("outcome=\"success\"");
        assertThat(response.getBody()).contains("outcome=\"failure\"");
        assertThat(response.getBody()).contains("reason=\"UserAlreadyUsedCouponException\"");
    }

    private HttpHeaders jsonHeaders(String xForwardedFor) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Forwarded-For", xForwardedFor);
        return headers;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CapturingGeoIpConfiguration {

        @Bean
        @Primary
        CapturingGeoIpService capturingGeoIpService() {
            return new CapturingGeoIpService();
        }
    }

    static class CapturingGeoIpService implements GeoIpService {

        private final AtomicReference<String> lastResolvedIp = new AtomicReference<>();
        private final AtomicReference<String> countryCode = new AtomicReference<>("PL");

        @Override
        public Optional<String> resolveCountryCode(String ipAddress) {
            lastResolvedIp.set(ipAddress);
            return Optional.ofNullable(countryCode.get());
        }

        void resolveCountryAs(String countryCode) {
            this.countryCode.set(countryCode);
        }

        String lastResolvedIp() {
            return lastResolvedIp.get();
        }
    }
}
