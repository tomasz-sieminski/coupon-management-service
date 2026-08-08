# ADR 0006: Use GitHub Actions and Spotless for quality gates

## Status

Accepted

## Context

The project should keep a consistent Java style and verify changes before merging.

The repository uses pull requests and branch protection as the collaboration model.

## Decision

Use GitHub Actions to build and test commits.

Use Spotless with Palantir Java Format to enforce Java formatting.

Run CI on pushes so every branch commit can produce a check result without duplicate pull request runs.

## Consequences

Formatting decisions are automated instead of being handled manually in review.

Every pushed commit can be validated by the same workflow.

Branch protection can require the CI check once the workflow has produced its first successful run.
