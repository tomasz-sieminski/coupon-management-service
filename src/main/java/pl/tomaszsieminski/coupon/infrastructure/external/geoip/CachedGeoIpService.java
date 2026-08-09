package pl.tomaszsieminski.coupon.infrastructure.external.geoip;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pl.tomaszsieminski.coupon.application.port.out.GeoIpService;
import pl.tomaszsieminski.coupon.infrastructure.external.geoip.provider.IpWhoIsGeoIpClient;

@Service
@ConditionalOnProperty(prefix = "app.geoip", name = "mode", havingValue = "external")
public class CachedGeoIpService implements GeoIpService {

    private final IpWhoIsGeoIpClient client;

    public CachedGeoIpService(IpWhoIsGeoIpClient client) {
        this.client = client;
    }

    @Override
    @Cacheable(value = "geoip", key = "#ipAddress", unless = "#result == null")
    public Optional<String> resolveCountryCode(String ipAddress) {
        return client.fetchCountryCode(ipAddress);
    }
}
