package pl.tomaszsieminski.coupon.application.port.out;

import java.util.Optional;

public interface GeoIpService {
    Optional<String> resolveCountryCode(String ipAddress);
}
