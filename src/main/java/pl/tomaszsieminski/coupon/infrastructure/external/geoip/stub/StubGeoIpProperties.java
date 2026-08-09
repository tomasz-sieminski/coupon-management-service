package pl.tomaszsieminski.coupon.infrastructure.external.geoip.stub;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geoip.stub")
public record StubGeoIpProperties(String countryCode, Map<String, String> countryByIp) {

    public StubGeoIpProperties {
        countryByIp = countryByIp == null ? Map.of() : Map.copyOf(countryByIp);
    }
}
