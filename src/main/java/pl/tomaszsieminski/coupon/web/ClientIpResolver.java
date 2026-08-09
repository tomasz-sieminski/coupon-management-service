package pl.tomaszsieminski.coupon.web;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private final TrustedProxyProperties properties;

    public ClientIpResolver(TrustedProxyProperties properties) {
        this.properties = properties;
    }

    public String resolve(String xForwardedFor, String remoteAddress) {
        if (isTrustedProxy(remoteAddress) && xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        List<String> trustedProxies = properties.getTrustedProxies();
        return trustedProxies.stream().anyMatch(remoteAddress::equals);
    }
}
