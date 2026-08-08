# ADR 0005: Use Testcontainers for persistence tests

## Status

Accepted

## Context

The persistence layer relies on PostgreSQL-specific behavior, Flyway migrations, constraints, and SQL update semantics.

An in-memory database would not provide the same guarantees as the production database.

## Decision

Run persistence and integration tests against PostgreSQL using Testcontainers.

Share Testcontainers setup through test configuration instead of duplicating container setup in each test class.

## Consequences

Tests validate real database behavior, including migrations and constraints.

The test suite requires Docker for integration tests.

Tests are heavier than pure unit tests, so database tests should stay focused on persistence and integration behavior.
