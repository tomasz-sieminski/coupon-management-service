# Coupon Service

REST API for creating and redeeming discount coupons with usage limits, country validation via GeoIP, and Prometheus/Grafana observability.

## Quick Start

### Development

The fastest feedback loop — runs the application locally with only PostgreSQL in Docker:

```bash
docker compose up -d postgres
./gradlew bootRun
```

The application starts on `http://localhost:8080` with external GeoIP (`ipwho.is`) and no trusted proxies.

To trust local loopback addresses for `X-Forwarded-For` testing:

```bash
SPRING_APPLICATION_JSON='{"app":{"web":{"trusted-proxies":["127.0.0.1","::1"]}}}' ./gradlew bootRun
```

### Docker — production-like (1 instance + monitoring)

```bash
./run.sh prod
```

| Endpoint | URL |
|----------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Grafana | http://localhost:3000 (admin / admin) |
| Prometheus | http://localhost:9090 |

GeoIP mode: `external` (`ipwho.is`, 1000 req/day limit per client IP).

### Docker — load testing (2 instances + Nginx + monitoring)

```bash
# Start the stack
./run.sh load-test

# Open Grafana, then run k6
./run.sh k6

# Optional: override coupon limit
./run.sh k6 COUPON_MAX_USES=500
```

Uses stub GeoIP with deterministic IP → country mappings (no external calls).

To run with real GeoIP provider instead:

```bash
./run.sh load-test-external
./run.sh k6
```

See [docs/load-testing.md](docs/load-testing.md) for full load testing documentation.

### Stop everything

```bash
./run.sh down
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/coupons` | Create a coupon |
| `POST` | `/api/v1/coupons/{code}/redeem` | Redeem a coupon |

### Create a coupon

```bash
curl -s -X POST http://localhost:8080/api/v1/coupons \
  -H "Content-Type: application/json" \
  -d '{"code": "WELCOME", "maxUses": 5, "countryCode": "US"}' | jq
```

Response `201 Created`:

```json
{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "code": "WELCOME",
  "maxUses": 5,
  "currentUses": 0,
  "countryCode": "US"
}
```

### Redeem a coupon

```bash
curl -s -X POST http://localhost:8080/api/v1/coupons/WELCOME/redeem \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 8.8.8.8" \
  -d '{"userId": "user-1"}'
```

Response `204 No Content` on success.

The country is resolved from the client IP using GeoIP. The header `X-Forwarded-For` is trusted only from proxies listed in `app.web.trusted-proxies`.

### Response codes

| Status | Meaning |
|--------|---------|
| `201 Created` | Coupon created |
| `204 No Content` | Coupon redeemed successfully |
| `400 Bad Request` | Validation failed |
| `403 Forbidden` | Client country does not match coupon country |
| `404 Not Found` | Coupon not found |
| `409 Conflict` | User already redeemed this coupon |
| `422 Unprocessable Content` | Coupon usage limit reached |
| `503 Service Unavailable` | GeoIP country resolution failed |

## Architecture Decisions

Documented in [`docs/adr/`](docs/adr/):

| ADR | Decision |
|-----|----------|
| [0001](docs/adr/0001-use-hexagonal-architecture.md) | Hexagonal architecture — domain and application isolated from infrastructure |
| [0002](docs/adr/0002-use-spring-mvc-jpa-and-virtual-threads.md) | Spring MVC + JPA + virtual threads — blocking I/O, simple imperative model |
| [0003](docs/adr/0003-use-flyway-for-database-schema-management.md) | Flyway for schema management — versioned migrations, `ddl-auto=validate` |
| [0004](docs/adr/0004-use-postgresql-for-coupon-consistency.md) | PostgreSQL as consistency boundary — atomic SQL updates, DB-enforced constraints |
| [0005](docs/adr/0005-use-testcontainers-for-persistence-tests.md) | Testcontainers — tests run against real PostgreSQL |
| [0006](docs/adr/0006-use-github-actions-and-spotless-for-quality-gates.md) | GitHub Actions + Spotless — CI on every push, automated formatting |
| [0007](docs/adr/0007-use-resilience4j-and-caffeine-for-geoip.md) | Resilience4j + Caffeine — retry, circuit breaker, and caching for GeoIP |
| [0008](docs/adr/0008-geoip-mode-property.md) | GeoIP adapter selection — `app.geoip.mode` switches between `external` and `stub` |

## AI Assistance

This project was developed with LLM assistance (Gemini Flash, Gemini Pro, GPT-5.5).

AI was used as a discussion partner, reviewer, and for scaffolding:
- Architecture and design discussions (hexagonal layout, concurrency strategy)
- Technology research for tools I hadn't used before (k6, Spotless, CodeRabbit)
- Breaking down the project into issues and verifying task decomposition
- Code review and debugging of my implementations
- Generating boilerplate code (entity mappings, configuration classes, Docker/CI setup)
- Generating k6 load test scripts (technology was new to me; reviewed and adapted based on Gatling experience)

All architectural decisions, design trade-offs, and final code are my own.
