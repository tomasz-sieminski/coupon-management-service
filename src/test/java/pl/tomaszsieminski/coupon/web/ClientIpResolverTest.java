package pl.tomaszsieminski.coupon.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClientIpResolverTest {

    @Test
    void shouldUseFirstForwardedAddressFromTrustedProxy() {
        ClientIpResolver resolver = resolverWithTrustedProxies("10.0.0.10");

        String clientIp = resolver.resolve("203.0.113.10, 10.0.0.1", "10.0.0.10");

        assertThat(clientIp).isEqualTo("203.0.113.10");
    }

    @Test
    void shouldIgnoreForwardedAddressFromUntrustedRemoteAddress() {
        ClientIpResolver resolver = resolverWithTrustedProxies("10.0.0.10");

        String clientIp = resolver.resolve("203.0.113.10", "198.51.100.20");

        assertThat(clientIp).isEqualTo("198.51.100.20");
    }

    private ClientIpResolver resolverWithTrustedProxies(String... trustedProxies) {
        TrustedProxyProperties properties = new TrustedProxyProperties();
        properties.setTrustedProxies(List.of(trustedProxies));
        return new ClientIpResolver(properties);
    }
}
