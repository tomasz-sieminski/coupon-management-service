package pl.tomaszsieminski.coupon.domain.exception;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(String code) {
        super("Coupon not found for code: " + code);
    }
}
