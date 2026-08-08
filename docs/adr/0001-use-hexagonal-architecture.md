# ADR 0001: Use hexagonal architecture

## Status

Accepted

## Context

The service exposes coupon management use cases through HTTP, persists data in PostgreSQL, and will integrate with external GeoIP providers.

The core application logic should not depend directly on web controllers, JPA repositories, HTTP clients, cache, or provider-specific APIs.

## Decision

Use a lightweight hexagonal architecture:

- domain model and domain exceptions in `domain`
- use case orchestration in `application`
- outbound ports in `application.port.out`
- infrastructure adapters in `infrastructure`
- web adapters in `web`

## Consequences

Application code depends on ports instead of technical details.

Infrastructure can be replaced or tested independently, for example replacing a real GeoIP provider with a stub.

The codebase has more classes and interfaces than a simple layered CRUD application, so boundaries must stay intentional.
