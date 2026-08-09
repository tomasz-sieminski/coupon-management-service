package pl.tomaszsieminski.coupon.infrastructure.external.geoip.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geoip.external")
public record IpWhoIsGeoIpProperties(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {}
