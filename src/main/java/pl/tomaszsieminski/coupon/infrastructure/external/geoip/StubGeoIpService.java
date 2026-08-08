package pl.tomaszsieminski.coupon.infrastructure.external.geoip;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import pl.tomaszsieminski.coupon.application.port.out.GeoIpService;
import pl.tomaszsieminski.coupon.infrastructure.external.geoip.stub.StubGeoIpProperties;

@Service
@ConditionalOnProperty(prefix = "app.geoip", name = "mode", havingValue = "stub")
public class StubGeoIpService implements GeoIpService {

    private final StubGeoIpProperties properties;

    public StubGeoIpService(StubGeoIpProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> resolveCountryCode(String ipAddress) {
        return Optional.ofNullable(properties.countryCode());
    }
}
