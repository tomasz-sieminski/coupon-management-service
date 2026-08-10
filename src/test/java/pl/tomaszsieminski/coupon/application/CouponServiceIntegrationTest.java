package pl.tomaszsieminski.coupon.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.tomaszsieminski.coupon.domain.exception.CouponExhaustedException;
import pl.tomaszsieminski.coupon.domain.exception.CouponNotFoundException;
import pl.tomaszsieminski.coupon.domain.exception.UserAlreadyUsedCouponException;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponEntity;
import pl.tomaszsieminski.coupon.support.FullStackIntegrationTestSupport;

class CouponServiceIntegrationTest extends FullStackIntegrationTestSupport {

    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("Should reject redemption when coupon does not exist")
    void shouldRejectRedemptionWhenCouponDoesNotExist() {
        assertThatThrownBy(() -> couponService.redeemCoupon("UNKNOWN", "user-1", "PL"))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("Should reject second redemption by the same user")
    void shouldRejectSecondRedemptionByTheSameUser() {
        givenCoupon("ONCE", 5, "PL");

        couponService.redeemCoupon("once", "user-1", "PL");

        assertThatThrownBy(() -> couponService.redeemCoupon("ONCE", "user-1", "PL"))
                .isInstanceOf(UserAlreadyUsedCouponException.class);

        CouponEntity coupon = couponRepository.findByCodeIgnoreCase("ONCE").orElseThrow();
        assertThat(coupon.getCurrentUses()).isEqualTo(1);
        assertThat(redemptionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject redemption when coupon usage limit is reached")
    void shouldRejectRedemptionWhenCouponUsageLimitIsReached() {
        givenCoupon("LIMIT", 1, "PL");

        couponService.redeemCoupon("limit", "user-1", "PL");

        assertThatThrownBy(() -> couponService.redeemCoupon("LIMIT", "user-2", "PL"))
                .isInstanceOf(CouponExhaustedException.class);

        CouponEntity coupon = couponRepository.findByCodeIgnoreCase("LIMIT").orElseThrow();
        assertThat(coupon.getCurrentUses()).isEqualTo(1);
        assertThat(redemptionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should not exceed coupon usage limit under concurrent redemptions")
    void shouldNotExceedCouponUsageLimitUnderConcurrentRedemptions() throws InterruptedException, ExecutionException {
        int maxUses = 10;
        int attempts = 50;
        givenCoupon("FLASH", maxUses, "PL");

        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(attempts);

        try {
            List<Future<Boolean>> results = IntStream.range(0, attempts)
                    .mapToObj(index -> executor.submit(() -> redeemConcurrently(index, ready, start)))
                    .toList();

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            long successfulRedemptions = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successfulRedemptions++;
                }
            }

            CouponEntity coupon = couponRepository.findByCodeIgnoreCase("flash").orElseThrow();
            assertThat(successfulRedemptions).isEqualTo(maxUses);
            assertThat(coupon.getCurrentUses()).isEqualTo(maxUses);
            assertThat(redemptionRepository.count()).isEqualTo(maxUses);
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean redeemConcurrently(int index, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();

        try {
            couponService.redeemCoupon("flash", "user-" + index, "PL");
            return true;
        } catch (CouponExhaustedException exception) {
            return false;
        }
    }
}
