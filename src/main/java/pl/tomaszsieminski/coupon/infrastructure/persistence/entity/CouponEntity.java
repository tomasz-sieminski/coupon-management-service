package pl.tomaszsieminski.coupon.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons")
public class CouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    private Integer currentUses = 0;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected CouponEntity() {}

    public CouponEntity(String code, Integer maxUses, String countryCode) {
        this.code = code;
        this.maxUses = maxUses;
        this.countryCode = countryCode;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public Integer getCurrentUses() {
        return currentUses;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
