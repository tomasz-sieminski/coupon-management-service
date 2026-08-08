package pl.tomaszsieminski.coupon.infrastructure.external.geoip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import pl.tomaszsieminski.coupon.application.port.out.GeoIpService;
import pl.tomaszsieminski.coupon.infrastructure.external.geoip.provider.IpApiGeoIpClient;

class GeoIpConfigurationTest {

    private static final String[] EXTERNAL_GEOIP_PROPERTIES = {
        "app.geoip.mode=external",
        "app.geoip.external.base-url=https://ipapi.co",
        "app.geoip.external.connect-timeout-ms=500",
        "app.geoip.external.read-timeout-ms=1000"
    };

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
    @DisplayName("Should use cached GeoIP service when external mode is configured")
    void shouldUseCachedGeoIpServiceWhenExternalModeIsConfigured() {
        IpApiGeoIpClient client = mock(IpApiGeoIpClient.class);

        contextRunner
                .withBean(IpApiGeoIpClient.class, () -> client)
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
        IpApiGeoIpClient client = mock(IpApiGeoIpClient.class);
        when(client.fetchCountryCode("1.1.1.1")).thenReturn(Optional.of("AU"));

        contextRunner
                .withBean(IpApiGeoIpClient.class, () -> client)
                .withPropertyValues(EXTERNAL_GEOIP_PROPERTIES)
                .run(context -> {
                    GeoIpService geoIpService = context.getBean(GeoIpService.class);

                    assertThat(geoIpService.resolveCountryCode("1.1.1.1")).contains("AU");
                    assertThat(geoIpService.resolveCountryCode("1.1.1.1")).contains("AU");

                    verify(client, times(1)).fetchCountryCode("1.1.1.1");
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
