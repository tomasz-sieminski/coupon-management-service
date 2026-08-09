package pl.tomaszsieminski.coupon.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszsieminski.coupon.application.port.out.CouponRepository;
import pl.tomaszsieminski.coupon.application.port.out.GeoIpService;
import pl.tomaszsieminski.coupon.domain.Coupon;
import pl.tomaszsieminski.coupon.domain.exception.CouponCountryMismatchException;
import pl.tomaszsieminski.coupon.domain.exception.CouponExhaustedException;
import pl.tomaszsieminski.coupon.domain.exception.CouponNotFoundException;
import pl.tomaszsieminski.coupon.domain.exception.GeoIpCountryResolutionException;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final GeoIpService geoIpService;

    public CouponService(CouponRepository couponRepository, GeoIpService geoIpService) {
        this.couponRepository = couponRepository;
        this.geoIpService = geoIpService;
    }

    @Transactional
    public Coupon createCoupon(String code, int maxUses, String countryCode) {
        Coupon coupon = new Coupon(null, code, maxUses, 0, countryCode.toUpperCase());
        return couponRepository.save(coupon);
    }

    @Transactional
    public void redeemCoupon(String code, String userId, String clientIp) {
        Coupon coupon =
                couponRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new CouponNotFoundException(code));

        String userCountryCode = geoIpService
                .resolveCountryCode(clientIp)
                .orElseThrow(() -> new GeoIpCountryResolutionException(clientIp));

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
