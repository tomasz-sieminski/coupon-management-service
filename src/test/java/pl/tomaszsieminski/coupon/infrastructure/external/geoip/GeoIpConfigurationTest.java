package pl.tomaszsieminski.coupon.infrastructure.external.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import pl.tomaszsieminski.coupon.application.port.out.GeoIpService;
import pl.tomaszsieminski.coupon.infrastructure.external.geoip.provider.IpWhoIsGeoIpClient;

class GeoIpConfigurationTest {

    private static final String[] EXTERNAL_GEOIP_PROPERTIES = {
        "app.geoip.mode=external",
        "app.geoip.external.base-url=https://ipwho.is",
        "app.geoip.external.connect-timeout-ms=500",
        "app.geoip.external.read-timeout-ms=1000"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(
                    GeoIpConfiguration.class,
                    CachedGeoIpService.class,
                    StubGeoIpService.class,
                    GeoIpTestConfiguration.class);

    @Test
    @DisplayName("Should use stub GeoIP service when stub mode is configured")
    void shouldUseStubGeoIpServiceWhenStubModeIsConfigured() {
        contextRunner
                .withPropertyValues("app.geoip.mode=stub", "app.geoip.stub.country-code=PL")
                .run(context -> {
                    assertThat(context).hasSingleBean(GeoIpService.class);
                    assertThat(context).hasSingleBean(StubGeoIpService.class);
                    assertThat(context).doesNotHaveBean(CachedGeoIpService.class);

                    GeoIpService geoIpService = context.getBean(GeoIpService.class);
                    assertThat(geoIpService.resolveCountryCode("1.1.1.1")).contains("PL");
                });
    }

    @Test
    @DisplayName("Should resolve stub GeoIP country by IP address before using default country")
    void shouldResolveStubGeoIpCountryByIpAddressBeforeUsingDefaultCountry() {
        contextRunner
                .withPropertyValues(
                        "app.geoip.mode=stub",
                        "app.geoip.stub.country-code=PL",
                        "app.geoip.stub.country-by-ip[198.51.100.20]=DE")
                .run(context -> {
                    GeoIpService geoIpService = context.getBean(GeoIpService.class);

                    assertThat(geoIpService.resolveCountryCode("198.51.100.20")).contains("DE");
                    assertThat(geoIpService.resolveCountryCode("203.0.113.10")).contains("PL");
                });
    }

    @Test
    @DisplayName("Should use cached GeoIP service when external mode is configured")
    void shouldUseCachedGeoIpServiceWhenExternalModeIsConfigured() {
        IpWhoIsGeoIpClient client = mock(IpWhoIsGeoIpClient.class);

        contextRunner
                .withBean(IpWhoIsGeoIpClient.class, () -> client)
                .withPropertyValues(EXTERNAL_GEOIP_PROPERTIES)
                .run(context -> {
                    assertThat(context).hasSingleBean(GeoIpService.class);
                    assertThat(context).hasBean("cachedGeoIpService");
                    assertThat(context).doesNotHaveBean(StubGeoIpService.class);
                });
    }

    @Test
    @DisplayName("Should cache GeoIP provider responses")
    void shouldCacheGeoIpProviderResponses() {
        IpWhoIsGeoIpClient client = mock(IpWhoIsGeoIpClient.class);
        when(client.fetchCountryCode("1.1.1.1")).thenReturn(Optional.of("AU"));

        contextRunner
                .withBean(IpWhoIsGeoIpClient.class, () -> client)
                .withPropertyValues(EXTERNAL_GEOIP_PROPERTIES)
                .run(context -> {
                    GeoIpService geoIpService = context.getBean(GeoIpService.class);

                    assertThat(geoIpService.resolveCountryCode("1.1.1.1")).contains("AU");
                    assertThat(geoIpService.resolveCountryCode("1.1.1.1")).contains("AU");

                    verify(client, times(1)).fetchCountryCode("1.1.1.1");
                });
    }

    @Test
    @DisplayName("Should not cache empty GeoIP provider responses")
    void shouldNotCacheEmptyGeoIpProviderResponses() {
        IpWhoIsGeoIpClient client = mock(IpWhoIsGeoIpClient.class);
        when(client.fetchCountryCode("8.8.8.8")).thenReturn(Optional.empty());

        contextRunner
                .withBean(IpWhoIsGeoIpClient.class, () -> client)
                .withPropertyValues(EXTERNAL_GEOIP_PROPERTIES)
                .run(context -> {
                    GeoIpService geoIpService = context.getBean(GeoIpService.class);

                    assertThat(geoIpService.resolveCountryCode("8.8.8.8")).isEmpty();
                    assertThat(geoIpService.resolveCountryCode("8.8.8.8")).isEmpty();

                    verify(client, times(2)).fetchCountryCode("8.8.8.8");
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GeoIpTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager("geoip");
        }
    }
}
