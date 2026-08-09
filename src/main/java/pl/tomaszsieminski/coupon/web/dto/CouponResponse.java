package pl.tomaszsieminski.coupon.web.dto;

import pl.tomaszsieminski.coupon.domain.Coupon;

public record CouponResponse(String id, String code, int maxUses, int currentUses, String countryCode) {

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.id().toString(), coupon.code(), coupon.maxUses(), coupon.currentUses(), coupon.countryCode());
    }
}
