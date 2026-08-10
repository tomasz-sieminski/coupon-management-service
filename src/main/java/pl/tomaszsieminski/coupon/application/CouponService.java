package pl.tomaszsieminski.coupon.application;

import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszsieminski.coupon.application.port.out.CouponRepository;
import pl.tomaszsieminski.coupon.domain.Coupon;
import pl.tomaszsieminski.coupon.domain.exception.CouponCountryMismatchException;
import pl.tomaszsieminski.coupon.domain.exception.CouponExhaustedException;
import pl.tomaszsieminski.coupon.domain.exception.CouponNotFoundException;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public Coupon createCoupon(String code, int maxUses, String countryCode) {
        Coupon coupon = new Coupon(null, code, maxUses, 0, countryCode.toUpperCase(Locale.ROOT));
        return couponRepository.save(coupon);
    }

    @Transactional
    public void redeemCoupon(String code, String userId, String userCountryCode) {
        Coupon coupon =
                couponRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new CouponNotFoundException(code));

        if (!coupon.countryCode().equalsIgnoreCase(userCountryCode)) {
            throw new CouponCountryMismatchException(code, coupon.countryCode(), userCountryCode);
        }

        couponRepository.saveRedemption(coupon, userId);

        int updatedRows = couponRepository.incrementUsesIfAvailable(code);
        if (updatedRows == 0) {
            throw new CouponExhaustedException(code);
        }
    }
}
