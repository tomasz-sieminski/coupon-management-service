# Load testing and observability

## Starting the stack

```bash
# Stub GeoIP (recommended — deterministic, no external calls)
./run.sh load-test

# External GeoIP (real ipwho.is provider — 1000 req/day limit)
./run.sh load-test-external
```

After changing application or Docker Compose configuration, restart the app containers before running k6:

```bash
./run.sh down
./run.sh load-test
```

Available endpoints once the stack is up:

| Endpoint | URL |
|----------|-----|
| API (through Nginx) | http://localhost:8080 |
| App instance 1 (direct) | http://localhost:8081 |
| App instance 2 (direct) | http://localhost:8082 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin) |

Prometheus scrapes application targets inside the Docker network:

- `http://app:8080/actuator/prometheus` (prod profile)
- `http://app1:8080/actuator/prometheus` (load-test profile)
- `http://app2:8080/actuator/prometheus` (load-test profile)
- `http://postgres-exporter:9187/metrics`

Grafana is provisioned with:

- Prometheus datasource: `http://prometheus:9090`
- Dashboard: `Coupon Service Load Test`

## Running k6

Open Grafana first, then run k6 so you can watch metrics in real time:

```bash
# Default (COUPON_MAX_USES=100)
./run.sh k6

# Override coupon limit
./run.sh k6 COUPON_MAX_USES=500
```

The k6 script creates fresh coupons, waits for app1 and app2 readiness, then runs three scenarios through Nginx:

- **redemption_race** — concurrent redemptions for one limited coupon
- **country_redemption** — successful redemptions for PL, DE, and US coupons
- **expected_error_resilience** — country mismatch, duplicate user, unknown coupon, invalid request

k6 report artifacts written after the run:

- HTML report: `reports/k6/redemption-race-report.html`
- Raw JSON summary: `reports/k6/redemption-race-summary.json`

The `reports/k6` directory is ignored by Git.

## GeoIP stub mappings

The load-test stack uses stub GeoIP with deterministic IP → country mappings:

| IP address | Country |
|------------|---------|
| `203.0.113.10` | PL |
| `198.51.100.20` | DE |
| `192.0.2.30` | US |

All other IPs resolve to the default country (`PL`).

This makes country scenarios deterministic without calling an external provider. Switch to `./run.sh load-test-external` to test with real GeoIP resolution.

## Business metrics

```
coupon_redemptions_total{outcome="success", reason="none"}
coupon_redemptions_total{outcome="failure", reason="<ExceptionClass>"}
```

## Cache metrics

Caffeine cache statistics require `recordStats` in the cache spec (enabled in `application.yaml`):

```
cache_gets_total{cache="geoip", result="hit"}
cache_gets_total{cache="geoip", result="miss"}
```

Cache metrics are meaningful only in `external` GeoIP mode. In `stub` mode the cache layer is bypassed.

## HTTP latency

HTTP p95 uses Prometheus histogram buckets from `http_server_requests_seconds_bucket`. Percentile histograms are enabled in `application.yaml`:

```yaml
management.metrics.distribution.percentiles-histogram.http.server.requests: true
```

Dashboard panels exclude actuator endpoints so the request rate and p95 reflect only API traffic.

## Database metrics

- `hikaricp_connections_active/idle/pending` — connection pool pressure
- `hikaricp_connections_acquire_seconds_*` — connection acquire latency
- `spring_data_repository_invocations_seconds_*` — repository method rate and response times
- `pg_stat_database_*` — PostgreSQL activity from postgres-exporter

## Nginx and X-Forwarded-For

The Nginx config in `docker/nginx/default.conf` preserves an incoming `X-Forwarded-For` header so k6 can simulate different client countries. This is intentional for load testing and would not be appropriate in a production reverse proxy.

## Troubleshooting

**Country scenario shows many `unexpected_redemption_response`:** Recreate the app containers — old containers may still use a single-country stub:

```bash
./run.sh down
./run.sh load-test
```

**GeoIP 503 in external mode:** The `ipwho.is` free tier has a 1000 req/day limit per client IP. Check cache metrics for hit/miss ratio. If the limit is exceeded, switch to `./run.sh load-test` (stub mode).
