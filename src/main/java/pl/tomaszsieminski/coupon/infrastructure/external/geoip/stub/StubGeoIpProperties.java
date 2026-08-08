package pl.tomaszsieminski.coupon.infrastructure.external.geoip.stub;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geoip.stub")
public record StubGeoIpProperties(String countryCode) {}
