package pl.tomaszsieminski.coupon.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private CouponEntity coupon;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "redeemed_at", nullable = false, updatable = false)
    private Instant redeemedAt = Instant.now();

    protected CouponRedemptionEntity() {}

    public CouponRedemptionEntity(CouponEntity coupon, String userId) {
        this.coupon = coupon;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public CouponEntity getCoupon() {
        return coupon;
    }

    public String getUserId() {
        return userId;
    }

    public Instant getRedeemedAt() {
        return redeemedAt;
    }
}
