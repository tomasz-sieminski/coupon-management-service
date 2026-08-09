package pl.tomaszsieminski.coupon.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateCouponRequest(
        @NotBlank(message = "Coupon code cannot be blank")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "Coupon code can contain only letters, numbers, underscores and hyphens")
        String code,

        @NotNull(message = "Maximum uses cannot be null") @Positive(message = "Maximum uses must be greater than zero") Integer maxUses,

        @NotBlank(message = "Country code cannot be blank")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "Country code must be a two-letter ISO code")
        String countryCode) {}
