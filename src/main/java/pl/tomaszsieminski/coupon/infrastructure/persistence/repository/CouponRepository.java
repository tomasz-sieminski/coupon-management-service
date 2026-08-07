package pl.tomaszsieminski.coupon.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponEntity;

public interface CouponRepository extends JpaRepository<CouponEntity, UUID> {

    Optional<CouponEntity> findByCodeIgnoreCase(String code);

    @Modifying
    @Query(
            "UPDATE CouponEntity c SET c.currentUses = c.currentUses + 1 WHERE UPPER(c.code) = UPPER(:code) AND c.currentUses < c.maxUses")
    int incrementUsesIfAvailable(@Param("code") String code);
}
