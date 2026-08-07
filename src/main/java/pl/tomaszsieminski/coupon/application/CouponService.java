package pl.tomaszsieminski.coupon.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszsieminski.coupon.application.port.out.CouponRepository;
import pl.tomaszsieminski.coupon.domain.Coupon;
import pl.tomaszsieminski.coupon.domain.exception.CouponExhaustedException;
import pl.tomaszsieminski.coupon.domain.exception.CouponNotFoundException;

@Service
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Transactional
    public void redeemCoupon(String code, String userId) {
        Coupon coupon =
                couponRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new CouponNotFoundException(code));

        couponRepository.saveRedemption(coupon, userId);

        int updatedRows = couponRepository.incrementUsesIfAvailable(code);
        if (updatedRows == 0) {
            throw new CouponExhaustedException(code);
        }
    }
}
