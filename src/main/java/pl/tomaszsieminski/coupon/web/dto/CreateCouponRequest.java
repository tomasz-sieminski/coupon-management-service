package pl.tomaszsieminski.coupon.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import pl.tomaszsieminski.coupon.web.validation.IsoCountryCode;

public record CreateCouponRequest(
        @Schema(example = "WELCOME")
        @NotBlank(message = "Coupon code cannot be blank")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "Coupon code can contain only letters, numbers, underscores and hyphens")
        String code,

        @Schema(example = "5")
        @NotNull(message = "Maximum uses cannot be null") @Positive(message = "Maximum uses must be greater than zero") Integer maxUses,

        @Schema(example = "US") @NotBlank(message = "Country code cannot be blank") @IsoCountryCode
        String countryCode) {}
