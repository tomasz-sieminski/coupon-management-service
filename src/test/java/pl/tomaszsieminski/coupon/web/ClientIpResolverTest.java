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

    @Test
    void shouldUseForwardedAddressFromTrustedProxySubnet() {
        ClientIpResolver resolver = resolverWithTrustedProxies("172.28.0.0/16");

        String clientIp = resolver.resolve("203.0.113.10", "172.28.0.12");

        assertThat(clientIp).isEqualTo("203.0.113.10");
    }

    @Test
    void shouldUseForwardedAddressFromTrustedIpv6Proxy() {
        ClientIpResolver resolver = resolverWithTrustedProxies("127.0.0.1", "::1", "0:0:0:0:0:0:0:1");

        String clientIp = resolver.resolve("8.8.8.8", "0:0:0:0:0:0:0:1");

        assertThat(clientIp).isEqualTo("8.8.8.8");
    }

    @Test
    void shouldUseIpv6ForwardedAddressFromTrustedProxy() {
        ClientIpResolver resolver = resolverWithTrustedProxies("127.0.0.1");

        String clientIp = resolver.resolve("2001:4860:4860::8888", "127.0.0.1");

        assertThat(clientIp).isEqualTo("2001:4860:4860::8888");
    }

    @Test
    void shouldUseForwardedAddressFromTrustedIpv6Subnet() {
        ClientIpResolver resolver = resolverWithTrustedProxies("2001:db8:abcd::/48");

        String clientIp = resolver.resolve("8.8.4.4", "2001:db8:abcd:12::1");

        assertThat(clientIp).isEqualTo("8.8.4.4");
    }

    @Test
    void shouldIgnoreInvalidForwardedAddress() {
        ClientIpResolver resolver = resolverWithTrustedProxies("127.0.0.1");

        String clientIp = resolver.resolve("localhost", "127.0.0.1");

        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldIgnoreHostnameTrustedProxy() {
        ClientIpResolver resolver = resolverWithTrustedProxies("localhost");

        String clientIp = resolver.resolve("8.8.8.8", "127.0.0.1");

        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

    private ClientIpResolver resolverWithTrustedProxies(String... trustedProxies) {
        TrustedProxyProperties properties = new TrustedProxyProperties();
        properties.setTrustedProxies(List.of(trustedProxies));
        return new ClientIpResolver(properties);
    }
}
