package pl.tomaszsieminski.coupon.infrastructure.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponRedemptionEntity;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemptionEntity, UUID> {}
