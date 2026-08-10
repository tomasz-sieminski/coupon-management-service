#!/usr/bin/env bash
set -euo pipefail

COMPOSE_CMD="docker compose"

usage() {
  echo "Usage: ./run.sh {prod|load-test|load-test-external|k6 [KEY=VALUE...]|down}"
  echo ""
  echo "  prod                  Start 1 app instance with external GeoIP + monitoring"
  echo "  load-test             Start 2 app instances with stub GeoIP + Nginx + monitoring"
  echo "  load-test-external    Same as load-test but with external GeoIP provider"
  echo "  k6 [KEY=VALUE...]     Run k6 load test against the running load-test stack"
  echo "                        Example: ./run.sh k6 COUPON_MAX_USES=500"
  echo "  down                  Stop and remove all containers"
}

case "${1:-help}" in
  prod)
    $COMPOSE_CMD --profile prod up --build -d
    echo ""
    echo "Stack is up:"
    echo "  API:        http://localhost:8080"
    echo "  Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "  Grafana:    http://localhost:3000  (admin / admin)"
    echo "  Prometheus: http://localhost:9090"
    ;;

  load-test)
    $COMPOSE_CMD --profile load-test up --build -d
    echo ""
    echo "Stack is up:"
    echo "  API (Nginx):  http://localhost:8080"
    echo "  App instance 1: http://localhost:8081"
    echo "  App instance 2: http://localhost:8082"
    echo "  Grafana:      http://localhost:3000  (admin / admin)"
    echo "  Prometheus:   http://localhost:9090"
    echo ""
    echo "Run load test with: ./run.sh k6"
    ;;

  load-test-external)
    $COMPOSE_CMD --profile load-test \
      -f docker-compose.yml \
      -f docker-compose.external.yml \
      up --build -d
    echo ""
    echo "Stack is up (external GeoIP mode):"
    echo "  API (Nginx):  http://localhost:8080"
    echo "  App instance 1: http://localhost:8081"
    echo "  App instance 2: http://localhost:8082"
    echo "  Grafana:      http://localhost:3000  (admin / admin)"
    echo "  Prometheus:   http://localhost:9090"
    echo ""
    echo "Run load test with: ./run.sh k6"
    ;;

  k6)
    shift
    env_args=()
    for arg in "$@"; do
      env_args+=(-e "$arg")
    done
    $COMPOSE_CMD --profile k6 run --rm "${env_args[@]}" k6
    ;;

  down)
    $COMPOSE_CMD --profile prod --profile load-test --profile k6 down -v
    ;;

  *)
    usage
    exit 1
    ;;
esac
