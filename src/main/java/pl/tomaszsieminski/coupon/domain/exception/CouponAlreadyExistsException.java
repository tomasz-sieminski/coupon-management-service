package pl.tomaszsieminski.coupon.domain.exception;

public class CouponAlreadyExistsException extends RuntimeException {

    public CouponAlreadyExistsException(String code) {
        super("Coupon already exists for code: " + code);
    }
}
