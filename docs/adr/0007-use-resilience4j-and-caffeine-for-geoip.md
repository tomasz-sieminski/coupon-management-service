# ADR 0007: Use Resilience4j and Caffeine for GeoIP

## Status

Accepted

## Context

Coupon validation may depend on the client country resolved from an IP address.

GeoIP resolution uses an external HTTP provider, which can be slow, unavailable, rate-limited, or return incomplete data.

Application logic should not know whether GeoIP data comes from HTTP, cache, or a stub.

## Decision

Expose GeoIP through the `GeoIpService` outbound port.

Use a Spring `RestClient` adapter for the external provider.

Use Resilience4j Retry and Circuit Breaker around provider calls.

Use Caffeine cache through Spring Cache to avoid repeated calls for the same IP address.

Keep cache and provider calls in separate Spring beans to avoid self-invocation bypassing Spring AOP.

## Consequences

The application depends only on the GeoIP port.

Provider failures can degrade to an empty result instead of breaking the whole request path.

Repeated lookups are served from memory and do not count against provider rate limits.

The integration has more moving parts and requires focused configuration tests.
