package pl.tomaszsieminski.coupon.infrastructure.external.geoip;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import pl.tomaszsieminski.coupon.infrastructure.external.geoip.provider.IpWhoIsGeoIpProperties;
import pl.tomaszsieminski.coupon.infrastructure.external.geoip.stub.StubGeoIpProperties;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties({IpWhoIsGeoIpProperties.class, StubGeoIpProperties.class})
public class GeoIpConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.geoip", name = "mode", havingValue = "external")
    RestClient ipWhoIsRestClient(RestClient.Builder builder, IpWhoIsGeoIpProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        return builder.requestFactory(requestFactory)
                .baseUrl(properties.baseUrl())
                .build();
    }
}
