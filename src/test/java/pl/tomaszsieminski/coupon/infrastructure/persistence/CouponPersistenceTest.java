package pl.tomaszsieminski.coupon.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import pl.tomaszsieminski.coupon.TestcontainersConfiguration;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponEntity;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponRedemptionEntity;
import pl.tomaszsieminski.coupon.infrastructure.persistence.repository.CouponRedemptionRepository;
import pl.tomaszsieminski.coupon.infrastructure.persistence.repository.CouponRepository;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CouponPersistenceTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponRedemptionRepository redemptionRepository;

    @Test
    @DisplayName("Should reject duplicate coupon codes")
    void shouldThrowExceptionWhenCouponCodeIsNotUnique() {
        CouponEntity coupon1 = new CouponEntity("WIOSNA", 10, "PL");
        CouponEntity coupon2 = new CouponEntity("WIOSNA", 20, "PL");

        couponRepository.saveAndFlush(coupon1);

        assertThatThrownBy(() -> couponRepository.saveAndFlush(coupon2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should reject coupon codes that differ only by case")
    void shouldThrowExceptionWhenCouponCodeDiffersOnlyByCase() {
        CouponEntity coupon1 = new CouponEntity("WINTER", 10, "PL");
        CouponEntity coupon2 = new CouponEntity("winter", 20, "PL");

        couponRepository.saveAndFlush(coupon1);

        assertThatThrownBy(() -> couponRepository.saveAndFlush(coupon2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find coupon by code regardless of case")
    void shouldFindCouponByCodeRegardlessOfCase() {
        CouponEntity coupon = new CouponEntity("AUTUMN", 10, "PL");
        couponRepository.saveAndFlush(coupon);

        assertThat(couponRepository.findByCodeIgnoreCase("autumn")).contains(coupon);
    }

    @Test
    @DisplayName("Should prevent the same user from redeeming the same coupon twice")
    void shouldPreventUserFromRedeemingSameCouponTwice() {
        CouponEntity coupon = new CouponEntity("LATO", 5, "PL");
        couponRepository.saveAndFlush(coupon);

        String userId = "user_123";

        CouponRedemptionEntity redemption1 = new CouponRedemptionEntity(coupon, userId);
        CouponRedemptionEntity redemption2 = new CouponRedemptionEntity(coupon, userId);

        redemptionRepository.saveAndFlush(redemption1);

        assertThatThrownBy(() -> redemptionRepository.saveAndFlush(redemption2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should not increment coupon uses after the coupon usage limit is reached")
    void shouldNotIncrementUsesWhenCouponIsExhausted() {
        CouponEntity coupon = new CouponEntity("PROMO", 1, "PL");
        couponRepository.saveAndFlush(coupon);

        int updatedRowsFirstTime = couponRepository.incrementUsesIfAvailable("PROMO");
        assertThat(updatedRowsFirstTime).isEqualTo(1);

        int updatedRowsSecondTime = couponRepository.incrementUsesIfAvailable("PROMO");
        assertThat(updatedRowsSecondTime).isEqualTo(0);
    }
}
