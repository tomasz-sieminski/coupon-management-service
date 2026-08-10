# Load testing and observability

Start the local load-test stack:

```shell
docker compose up --build postgres app1 app2 nginx postgres-exporter prometheus grafana
```

After changing application or Docker Compose configuration, recreate the app containers before running k6:

```shell
docker compose up --build --force-recreate -d postgres app1 app2 nginx postgres-exporter prometheus grafana
```

Available endpoints:

- API through Nginx: `http://localhost:8080`
- Direct application instance 1: `http://localhost:8081`
- Direct application instance 2: `http://localhost:8082`
- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000`
  - username: `admin`
  - password: `admin`

`app1` and `app2` are Docker-internal DNS names. Use `localhost:8081` and `localhost:8082` from the host browser.

Prometheus scrapes application targets inside the Docker network:

- `http://app1:8080/actuator/prometheus`
- `http://app2:8080/actuator/prometheus`
- `http://postgres-exporter:9187/metrics`

Grafana is provisioned with:

- Prometheus datasource: `http://prometheus:9090`
- dashboard: `Coupon Service Load Test`

Run the k6 load-test scenario:

```shell
docker compose --profile load-test run --rm k6
```

The k6 script creates fresh coupons, then runs three scenarios through Nginx:

- concurrent redemption race for one limited coupon,
- successful country-specific redemptions for PL, DE, and US,
- expected error-path checks for country mismatch, duplicate user, unknown coupon, and invalid request.

The `redemption_success` threshold verifies that successful redemptions do not exceed the configured coupon limit. HTTP
`400`, `403`, `404`, `409`, and `422` responses covered by the scenario are expected API responses and are not counted
as failed HTTP requests by the script. Unexpected statuses are counted with `unexpected_redemption_response`. The script
waits for app1 and app2 readiness before creating coupons, using `READINESS_URLS` from Docker Compose.

Useful environment overrides:

PowerShell:

```powershell
docker compose --profile load-test run --rm -e COUPON_MAX_USES=250 k6
```

Bash:

```bash
COUPON_MAX_USES=250 docker compose --profile load-test run --rm k6
```

Business metrics exposed in Prometheus format:

- `coupon_redemptions_total{outcome="success",reason="none"}`
- `coupon_redemptions_total{outcome="failure",reason="<ExceptionClass>"}`

Cache and latency metrics:

- `cache_gets_total{result="hit"}` / `cache_gets_total{result="miss"}` are exposed for instrumented Spring caches.
- Caffeine cache statistics require `recordStats`, which is enabled in `spring.cache.caffeine.spec`.
- HTTP p95 in Grafana uses Prometheus histogram buckets from `http_server_requests_seconds_bucket`; those buckets require
  `management.metrics.distribution.percentiles-histogram.http.server.requests=true`.
- HTTP dashboard panels exclude actuator endpoints, including `/actuator/health/**` and `/actuator/prometheus`, so the
  visible request rate and p95 focus on API traffic.

Database metrics:

- `hikaricp_connections_active`, `hikaricp_connections_idle`, and `hikaricp_connections_pending` show application-side
  connection pool pressure.
- `hikaricp_connections_acquire_seconds_*` and `hikaricp_connections_usage_seconds_*` show connection acquire and usage
  timings.
- `spring_data_repository_invocations_seconds_*` shows repository command rate and response times by repository method.
- `pg_stat_database_*` metrics come from `postgres-exporter` and show PostgreSQL activity, transactions, active backends,
  block reads/hits, deadlocks, and buffer cache hit ratio.
- `scrape_duration_seconds{job="postgres"}` shows Prometheus scrape response time for the database exporter.

The local Docker load-test stack uses GeoIP `stub` mode with deterministic IP-to-country mappings:

- `203.0.113.10` -> `PL`
- `198.51.100.20` -> `DE`
- `192.0.2.30` -> `US`

This makes the country scenarios deterministic without calling an external GeoIP provider. GeoIP cache hit/miss metrics
are meaningful when the cached `external` GeoIP adapter is used; in `stub` mode the cache layer is bypassed.

If the country scenario reports many `unexpected_redemption_response` values, first recreate the app containers. A common
cause is running k6 against old app containers that still use the previous single-country GeoIP stub configuration.

The local Nginx config preserves an incoming `X-Forwarded-For` header when k6 sends one, so the test can simulate
different client countries. Without that local test behavior, Nginx would replace the header with the k6 container IP and
all requests would resolve to the stub default country.

k6 report artifacts:

- HTML report: `reports/k6/redemption-race-report.html`
- Raw JSON summary: `reports/k6/redemption-race-summary.json`

The `reports/k6` directory is ignored by Git. Copy selected screenshots or exported reports into documentation only
when you want to keep a concrete run result.
