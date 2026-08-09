package pl.tomaszsieminski.coupon.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemCouponRequest(
        @NotBlank(message = "User ID cannot be blank") String userId) {}
