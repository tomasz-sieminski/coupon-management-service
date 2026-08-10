package pl.tomaszsieminski.coupon.infrastructure.external.geoip.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

class IpWhoIsGeoIpClientTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://ipwho.is");
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(restClientBuilder).build();
    private final IpWhoIsGeoIpClient client = new IpWhoIsGeoIpClient(restClientBuilder.build());

    @Test
    void shouldFetchCountryCodeUsingLimitedIpWhoIsFields() {
        server.expect(once(), this::assertCountryCodeRequest)
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "country_code": "US"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.fetchCountryCode("8.8.8.8")).contains("US");

        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenProviderReturnsUnsuccessfulResponse() {
        server.expect(once(), this::assertCountryCodeRequest)
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": false,
                          "message": "reserved range"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.fetchCountryCode("8.8.8.8")).isEmpty();

        server.verify();
    }

    @Test
    void shouldReturnEmptyWhenProviderDoesNotReturnCountryCode() {
        server.expect(once(), this::assertCountryCodeRequest)
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "success": true,
                          "country_code": ""
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.fetchCountryCode("8.8.8.8")).isEmpty();

        server.verify();
    }

    @Test
    void shouldThrowOnProviderHttpErrorSoResilience4jCanRetryAndFallback() {
        server.expect(once(), this::assertCountryCodeRequest)
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.fetchCountryCode("8.8.8.8")).isInstanceOf(HttpClientErrorException.class);

        server.verify();
    }

    private void assertCountryCodeRequest(org.springframework.http.client.ClientHttpRequest request) {
        URI uri = request.getURI();

        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("ipwho.is");
        assertThat(uri.getPath()).isEqualTo("/8.8.8.8");
        assertThat(uri.getQuery()).isEqualTo("fields=success,country_code,message");
    }
}
