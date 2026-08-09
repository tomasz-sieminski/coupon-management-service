package pl.tomaszsieminski.coupon.infrastructure.persistence;

import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import pl.tomaszsieminski.coupon.application.port.out.CouponRepository;
import pl.tomaszsieminski.coupon.domain.Coupon;
import pl.tomaszsieminski.coupon.domain.exception.CouponAlreadyExistsException;
import pl.tomaszsieminski.coupon.domain.exception.UserAlreadyUsedCouponException;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponEntity;
import pl.tomaszsieminski.coupon.infrastructure.persistence.entity.CouponRedemptionEntity;
import pl.tomaszsieminski.coupon.infrastructure.persistence.repository.JpaCouponRedemptionRepository;
import pl.tomaszsieminski.coupon.infrastructure.persistence.repository.JpaCouponRepository;

@Repository
public class CouponPersistenceAdapter implements CouponRepository {

    private static final String DUPLICATE_REDEMPTION_CONSTRAINT = "uk_coupon_user";
    private static final String DUPLICATE_COUPON_CODE_CONSTRAINT = "uk_coupons_code_lower";

    private final JpaCouponRepository couponRepository;
    private final JpaCouponRedemptionRepository redemptionRepository;

    public CouponPersistenceAdapter(
            JpaCouponRepository couponRepository, JpaCouponRedemptionRepository redemptionRepository) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        try {
            CouponEntity saved = couponRepository.saveAndFlush(
                    new CouponEntity(coupon.code(), coupon.maxUses(), coupon.countryCode()));
            return toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (isNotConstraintViolation(exception, DUPLICATE_COUPON_CODE_CONSTRAINT)) {
                throw exception;
            }
            throw new CouponAlreadyExistsException(coupon.code());
        }
    }

    @Override
    public Optional<Coupon> findByCodeIgnoreCase(String code) {
        return couponRepository.findByCodeIgnoreCase(code).map(this::toDomain);
    }

    @Override
    public void saveRedemption(Coupon coupon, String userId) {
        try {
            CouponEntity couponReference = couponRepository.getReferenceById(coupon.id());
            redemptionRepository.saveAndFlush(new CouponRedemptionEntity(couponReference, userId));
        } catch (DataIntegrityViolationException exception) {
            if (isNotConstraintViolation(exception, DUPLICATE_REDEMPTION_CONSTRAINT)) {
                throw exception;
            }
            throw new UserAlreadyUsedCouponException(userId, coupon.code());
        }
    }

    @Override
    public int incrementUsesIfAvailable(String code) {
        return couponRepository.incrementUsesIfAvailable(code);
    }

    private Coupon toDomain(CouponEntity entity) {
        return new Coupon(
                entity.getId(),
                entity.getCode(),
                entity.getMaxUses(),
                entity.getCurrentUses(),
                entity.getCountryCode());
    }

    private boolean isNotConstraintViolation(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                    && constraintName.equals(constraintViolationException.getConstraintName())) {
                return false;
            }
            current = current.getCause();
        }
        return true;
    }
}
