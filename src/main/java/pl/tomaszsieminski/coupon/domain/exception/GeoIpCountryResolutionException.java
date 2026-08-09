package pl.tomaszsieminski.coupon.domain.exception;

public class GeoIpCountryResolutionException extends RuntimeException {
    public GeoIpCountryResolutionException(String ipAddress) {
        super("Could not resolve country for IP address: " + ipAddress);
    }
}
