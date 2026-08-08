# ADR 0003: Use Flyway for database schema management

## Status

Accepted

## Context

Coupon uniqueness, redemption uniqueness, and usage limits are enforced by database constraints.

These rules are part of the production contract and must be repeatable across local development, CI, and deployed environments.

## Decision

Manage schema changes with Flyway migrations.

Use Hibernate with `ddl-auto=validate` instead of letting Hibernate create or update the production schema.

## Consequences

Schema changes are explicit, versioned, and reviewable.

The application fails fast if JPA mappings no longer match the database schema.

Developers must write migrations when changing persistence structures.
