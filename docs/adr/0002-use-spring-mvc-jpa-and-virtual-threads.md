# ADR 0002: Use Spring MVC, JPA, and virtual threads

## Status

Accepted

## Context

The service is request/response based, uses a relational database, and does not require streaming or high-volume asynchronous processing.

Spring Data JPA and JDBC are blocking APIs. A reactive stack would require different persistence technology and more complex control flow.

## Decision

Use Spring MVC with blocking I/O, Spring Data JPA, and Java virtual threads.

Enable virtual threads with `spring.threads.virtual.enabled=true`.

## Consequences

The application keeps a straightforward imperative programming model.

Blocking database and HTTP calls can run on virtual threads without requiring WebFlux or R2DBC.

External I/O still needs explicit connect and read timeouts, because virtual threads do not remove the need to bound slow network calls.
