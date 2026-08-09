package pl.tomaszsieminski.coupon.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.tomaszsieminski.coupon.application.CouponService;
import pl.tomaszsieminski.coupon.domain.Coupon;
import pl.tomaszsieminski.coupon.web.dto.CouponResponse;
import pl.tomaszsieminski.coupon.web.dto.CreateCouponRequest;
import pl.tomaszsieminski.coupon.web.dto.RedeemCouponRequest;

@RestController
@Validated
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
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

        String clientIp = extractClientIp(xForwardedFor, servletRequest);

        couponService.redeemCoupon(code, request.userId(), clientIp);

        return ResponseEntity.noContent().build();
    }

    private String extractClientIp(String xForwardedFor, HttpServletRequest request) {
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
