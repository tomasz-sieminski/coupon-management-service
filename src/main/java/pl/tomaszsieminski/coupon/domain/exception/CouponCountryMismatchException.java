package pl.tomaszsieminski.coupon.domain.exception;

public class CouponCountryMismatchException extends RuntimeException {
    public CouponCountryMismatchException(String code, String expectedCountry, String actualCountry) {
        super("Coupon " + code + " is valid for country " + expectedCountry + ", but client country is "
                + actualCountry);
    }
}
