package pl.tomaszsieminski.coupon.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pl.tomaszsieminski.coupon.TestcontainersConfiguration;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponEntity;
import pl.tomaszsieminski.coupon.infrastructure.persistence.repository.JpaCouponRedemptionRepository;
import pl.tomaszsieminski.coupon.infrastructure.persistence.repository.JpaCouponRepository;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "app.geoip.mode=stub",
            "app.geoip.stub.country-code=PL",
            "app.web.trusted-proxies[0]=127.0.0.1",
            "app.web.trusted-proxies[1]=::1",
            "app.web.trusted-proxies[2]=0:0:0:0:0:0:0:1"
        })
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
public abstract class FullStackIntegrationTestSupport {

    protected static final String CLIENT_IP = "8.8.8.8";

    @Autowired
    protected JpaCouponRepository couponRepository;

    @Autowired
    protected JpaCouponRedemptionRepository redemptionRepository;

    @BeforeEach
    void cleanDatabase() {
        redemptionRepository.deleteAll();
        couponRepository.deleteAll();
    }

    protected CouponEntity givenCoupon(String code, int maxUses, String countryCode) {
        return couponRepository.saveAndFlush(new CouponEntity(code, maxUses, countryCode));
    }
}
