# Coupon Service

REST API do tworzenia i realizowania kuponów z limitem użyć, walidacją kraju użytkownika przez GeoIP oraz metrykami Prometheus/Grafana.

## Wymagania

- Java 25
- Docker i Docker Compose
- PowerShell

## Uruchomienie w normalnym trybie

Normalny tryb aplikacji używa realnego zewnętrznego GeoIP providera (`ipwho.is`). Najprościej uruchomić bazę w Dockerze, a aplikację lokalnie przez Gradle.

Najpierw uruchom PostgreSQL:

```powershell
docker compose up -d postgres
```

Potem uruchom aplikację:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "postgres"
$env:SPRING_APPLICATION_JSON = '{"app":{"web":{"trusted-proxies":["127.0.0.1","::1"]}}}'
.\gradlew.bat bootRun
```

Domyślne ustawienia z `application.yaml`:

- baza: `jdbc:postgresql://127.0.0.1:5432/empik_coupons`
- GeoIP mode: `external`
- GeoIP provider: `https://ipwho.is`
- API: `http://127.0.0.1:8080`

`SPRING_APPLICATION_JSON` w przykładzie ufa lokalnym adresom loopback (`127.0.0.1`, `::1`) jako proxy tylko po to, żeby lokalnie można było testować `X-Forwarded-For`. W środowisku z realnym reverse proxy ustaw tam adres albo CIDR tego proxy.

Endpointy:

- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON: `http://127.0.0.1:8080/v3/api-docs`
- Health: `http://127.0.0.1:8080/actuator/health`
- Prometheus metrics: `http://127.0.0.1:8080/actuator/prometheus`

## Metryki

Metryki aplikacji są dostępne bezpośrednio z Actuatora:

```powershell
Invoke-WebRequest http://127.0.0.1:8080/actuator/prometheus
```

Dashboardy Grafany i stack load-testowy są opisane osobno w [docs/load-testing.md](docs/load-testing.md). Tamten tryb uruchamia dodatkowe kontenery, w tym Prometheusa, Grafanę, PostgreSQL exporter, Nginx i dwie instancje aplikacji.

Jeśli chcesz używać Grafany dla lokalnego `bootRun`, dodaj osobny target Prometheusa wskazujący na `host.docker.internal:8080/actuator/prometheus`.

## Korzystanie z API

Przykłady używają natywnych komend PowerShell. To omija problemy z cytowaniem JSON-a w `curl.exe` na Windows.

### Utworzenie kuponu

```powershell
$body = @{
  code = "WELCOME"
  maxUses = 5
  countryCode = "US"
} | ConvertTo-Json -Compress

Invoke-WebRequest `
  -Method Post `
  -Uri http://127.0.0.1:8080/api/v1/coupons `
  -ContentType "application/json" `
  -Body $body
```

Oczekiwany wynik: `201 Created`.

Przykładowa odpowiedź:

```json
{
  "id": "uuid",
  "code": "WELCOME",
  "maxUses": 5,
  "currentUses": 0,
  "countryCode": "US"
}
```

### Realizacja kuponu

Przykład redeem z publicznym IP Google DNS, które GeoIP provider powinien rozpoznać jako USA:

```powershell
$body = @{
  userId = "user-1"
} | ConvertTo-Json -Compress

Invoke-WebRequest `
  -Method Post `
  -Uri http://127.0.0.1:8080/api/v1/coupons/WELCOME/redeem `
  -ContentType "application/json" `
  -Headers @{ "X-Forwarded-For" = "8.8.8.8" } `
  -Body $body
```

Oczekiwany wynik dla kuponu `countryCode="US"`: `204 No Content`.

Analogiczny test IPv6:

```powershell
Invoke-WebRequest `
  -Method Post `
  -Uri http://127.0.0.1:8080/api/v1/coupons/WELCOME/redeem `
  -ContentType "application/json" `
  -Headers @{ "X-Forwarded-For" = "2001:4860:4860::8888" } `
  -Body (@{ userId = "user-ipv6" } | ConvertTo-Json -Compress)
```

## Typowe odpowiedzi API

- `201 Created` — kupon utworzony.
- `204 No Content` — kupon zrealizowany.
- `400 Bad Request` — niepoprawny JSON albo walidacja requestu.
- `403 Forbidden` — kraj klienta nie pasuje do kraju kuponu.
- `404 Not Found` — kupon nie istnieje.
- `409 Conflict` — ten sam użytkownik wykorzystał już kupon.
- `422 Unprocessable Content` — kupon osiągnął limit użyć.
- `503 Service Unavailable` — nie udało się rozpoznać kraju przez GeoIP.

## GeoIP external

Normalny tryb aplikacji to `app.geoip.mode=external`. W tym trybie kraj jest pobierany z `ipwho.is`. Domyślny tryb w `application.yaml` to `external`.

Aplikacja odpytuje provider tylko o pola `success,country_code,message`, bo do logiki kuponów potrzebny jest wyłącznie dwuliterowy kod kraju. `ipwho.is` obsługuje IPv4 i IPv6. Darmowy endpoint ma limit 1000 requestów dziennie na IP klienta.

W trybie `external` odpowiedzi GeoIP są cache’owane w cache `geoip`. Metryki cache możesz sprawdzić tak:

```powershell
(Invoke-WebRequest http://127.0.0.1:8080/actuator/prometheus).Content | Select-String "cache_gets_total"
```

Przykładowe metryki:

```text
cache_gets_total{application="coupon",cache="geoip",...,result="miss"} 1.0
cache_gets_total{application="coupon",cache="geoip",...,result="hit"} 1.0
```

Pierwsze sprawdzenie danego IP powinno zwiększyć `miss`, kolejne sprawdzenia tego samego IP powinny zwiększać `hit`.

Jeśli redeem w trybie `external` zwraca `503 Service Unavailable`, sprawdź metryki klienta HTTP:

```powershell
(Invoke-WebRequest http://127.0.0.1:8080/actuator/prometheus).Content | Select-String "http_client_requests_seconds"
```

Status `429` przy `client_name="ipwho.is"` oznacza limit po stronie zewnętrznego GeoIP providera. Wtedy aplikacja działa poprawnie technicznie: wywołuje realny provider, ale provider odmawia odpowiedzi i aplikacja zwraca `503`.

## Testy i formatowanie

```powershell
.\gradlew.bat test
.\gradlew.bat spotlessApply
```

