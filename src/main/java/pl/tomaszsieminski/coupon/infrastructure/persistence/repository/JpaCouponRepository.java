package pl.tomaszsieminski.coupon.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponEntity;

public interface JpaCouponRepository extends JpaRepository<CouponEntity, UUID> {

    @Query("SELECT c FROM CouponEntity c WHERE LOWER(c.code) = LOWER(:code)")
    Optional<CouponEntity> findByCodeIgnoreCase(@Param("code") String code);

    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE CouponEntity c SET c.currentUses = c.currentUses + 1 WHERE LOWER(c.code) = LOWER(:code) AND c.currentUses < c.maxUses")
    int incrementUsesIfAvailable(@Param("code") String code);
}
