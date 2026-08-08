package pl.tomaszsieminski.coupon.domain.exception;

public class CouponExhaustedException extends RuntimeException {

    public CouponExhaustedException(String code) {
        super("Coupon usage limit has been reached for code: " + code);
    }
}
