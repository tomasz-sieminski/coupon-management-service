# ADR 0004: Use PostgreSQL for coupon consistency

## Status

Accepted

## Context

Coupon redemption must prevent over-allocation under concurrent traffic.

The same user must not redeem the same coupon more than once.

Coupon codes are case-insensitive, so `WIOSNA` and `wiosna` represent the same coupon.

## Decision

Use PostgreSQL as the consistency boundary.

Enforce coupon code uniqueness through a case-insensitive database constraint.

Enforce one redemption per user with a unique constraint on `(coupon_id, user_id)`.

Increment coupon usage with an atomic SQL update guarded by `current_uses < max_uses`.

## Consequences

The service does not need Redis locks or application-level synchronization for coupon limits.

Concurrency correctness is enforced close to the data.

The application must translate database constraint violations into domain-level errors carefully.
