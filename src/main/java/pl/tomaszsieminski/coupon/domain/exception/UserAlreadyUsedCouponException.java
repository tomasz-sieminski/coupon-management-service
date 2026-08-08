package pl.tomaszsieminski.coupon.domain.exception;

public class UserAlreadyUsedCouponException extends RuntimeException {

    public UserAlreadyUsedCouponException(String userId, String code) {
        super("User " + userId + " has already used coupon: " + code);
    }
}
