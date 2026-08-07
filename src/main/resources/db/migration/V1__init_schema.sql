CREATE TABLE coupons
(
    id           UUID PRIMARY KEY,
    code         VARCHAR(50)              NOT NULL,
    max_uses     INT                      NOT NULL,
    current_uses INT                      NOT NULL DEFAULT 0,
    country_code VARCHAR(2)               NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_coupon_uses_limit CHECK (current_uses <= max_uses)
);

CREATE UNIQUE INDEX uk_coupons_code_lower ON coupons (LOWER(code));

CREATE TABLE coupon_redemptions
(
    id          UUID PRIMARY KEY,
    coupon_id   UUID                     NOT NULL REFERENCES coupons (id),
    user_id     VARCHAR(100)             NOT NULL,
    redeemed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_coupon_user UNIQUE (coupon_id, user_id)
);
