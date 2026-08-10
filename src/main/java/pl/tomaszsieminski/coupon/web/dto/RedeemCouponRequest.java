package pl.tomaszsieminski.coupon.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RedeemCouponRequest(
        @Schema(example = "user-1") @NotBlank(message = "User ID cannot be blank")
        String userId) {}
