package pl.tomaszsieminski.coupon.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.tomaszsieminski.coupon.application.CouponMetrics;
import pl.tomaszsieminski.coupon.application.CouponService;
import pl.tomaszsieminski.coupon.application.port.out.GeoIpService;
import pl.tomaszsieminski.coupon.domain.Coupon;
import pl.tomaszsieminski.coupon.domain.exception.GeoIpCountryResolutionException;
import pl.tomaszsieminski.coupon.web.dto.CouponResponse;
import pl.tomaszsieminski.coupon.web.dto.CreateCouponRequest;
import pl.tomaszsieminski.coupon.web.dto.RedeemCouponRequest;

@RestController
@Validated
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CouponMetrics couponMetrics;
    private final ClientIpResolver clientIpResolver;
    private final GeoIpService geoIpService;

    public CouponController(
            CouponService couponService,
            CouponMetrics couponMetrics,
            ClientIpResolver clientIpResolver,
            GeoIpService geoIpService) {
        this.couponService = couponService;
        this.couponMetrics = couponMetrics;
        this.clientIpResolver = clientIpResolver;
        this.geoIpService = geoIpService;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        Coupon coupon = couponService.createCoupon(request.code(), request.maxUses(), request.countryCode());

        URI location = URI.create("/api/v1/coupons/" + coupon.code());

        return ResponseEntity.created(location).body(CouponResponse.from(coupon));
    }

    @PostMapping("/{code}/redeem")
    public ResponseEntity<Void> redeemCoupon(
            @PathVariable @NotBlank(message = "Coupon code cannot be blank") String code,
            @Valid @RequestBody RedeemCouponRequest request,
            @RequestHeader(value = "X-Forwarded-For", required = false) String xForwardedFor,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolve(xForwardedFor, servletRequest.getRemoteAddr());

        try (MDC.MDCCloseable ignoredUserId = MDC.putCloseable("userId", request.userId());
                MDC.MDCCloseable ignoredCouponCode = MDC.putCloseable("couponCode", code)) {
            try {
                String userCountryCode = geoIpService
                        .resolveCountryCode(clientIp)
                        .orElseThrow(() -> new GeoIpCountryResolutionException(clientIp));

                couponService.redeemCoupon(code, request.userId(), userCountryCode);
                couponMetrics.recordSuccessfulRedemption();
            } catch (RuntimeException exception) {
                couponMetrics.recordFailedRedemption(exception);
                throw exception;
            }
        }

        return ResponseEntity.noContent().build();
    }
}
