# ADR 0008: GeoIP adapter selection

## Status

Accepted

## Context

The application needs two GeoIP implementations:

- `external` for the real HTTP provider integration
- `stub` for controlled local, test, and load-test environments

Spring beans are selected with `@ConditionalOnProperty` using `app.geoip.mode`.

## Decision

Use `app.geoip.mode` directly in `@ConditionalOnProperty` and do not introduce a separate `GeoIpProperties` record or `GeoIpMode` enum while the value is not consumed by application code.

Provider-specific settings remain strongly typed:

- `IpApiGeoIpProperties` for the external provider
- `StubGeoIpProperties` for the stub implementation

## Consequences

The configuration stays simple and avoids unused production code.

The accepted values of `app.geoip.mode` are documented here and covered by configuration tests.
