package pl.tomaszsieminski.coupon.domain;

import java.util.UUID;

public record Coupon(UUID id, String code, int maxUses, int currentUses, String countryCode) {}
