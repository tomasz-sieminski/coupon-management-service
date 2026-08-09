package pl.tomaszsieminski.coupon.application.port.out;

import java.util.Optional;
import pl.tomaszsieminski.coupon.domain.Coupon;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCodeIgnoreCase(String code);

    void saveRedemption(Coupon coupon, String userId);

    int incrementUsesIfAvailable(String code);
}
