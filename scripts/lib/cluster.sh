#!/usr/bin/env bash
# Shared helpers for OpenShift / MicroShift scripts. Source this file; do not execute it.

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Required command not found: $1" >&2
    exit 1
  fi
}

kube() {
  if command -v oc >/dev/null 2>&1; then
    oc "$@"
  elif command -v kubectl >/dev/null 2>&1; then
    kubectl "$@"
  else
    echo "oc or kubectl is required" >&2
    exit 1
  fi
}

load_env_file() {
  local root="$1"
  if [[ -f "${root}/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "${root}/.env"
    set +a
  fi
}

apply_commercehub_secrets() {
  local ns="${1:-commercehub}"
  : "${USER_DB_PASSWORD:?set USER_DB_PASSWORD in .env}"
  : "${PRODUCT_DB_PASSWORD:?set PRODUCT_DB_PASSWORD in .env}"
  : "${ORDER_DB_PASSWORD:?set ORDER_DB_PASSWORD in .env}"
  : "${INVENTORY_DB_PASSWORD:?set INVENTORY_DB_PASSWORD in .env}"
  : "${DEMO_ADMIN_EMAIL:?set DEMO_ADMIN_EMAIL in .env}"
  : "${DEMO_ADMIN_PASSWORD:?set DEMO_ADMIN_PASSWORD in .env}"
  local full_name="${DEMO_ADMIN_FULL_NAME:-CommerceHub Admin}"
  kube create secret generic commercehub-secrets \
    --namespace "${ns}" \
    --from-literal=USER_DB_PASSWORD="${USER_DB_PASSWORD}" \
    --from-literal=PRODUCT_DB_PASSWORD="${PRODUCT_DB_PASSWORD}" \
    --from-literal=ORDER_DB_PASSWORD="${ORDER_DB_PASSWORD}" \
    --from-literal=INVENTORY_DB_PASSWORD="${INVENTORY_DB_PASSWORD}" \
    --from-literal=DEMO_ADMIN_EMAIL="${DEMO_ADMIN_EMAIL}" \
    --from-literal=DEMO_ADMIN_PASSWORD="${DEMO_ADMIN_PASSWORD}" \
    --from-literal=DEMO_ADMIN_FULL_NAME="${full_name}" \
    --dry-run=client -o yaml | kube apply -f -
}

apply_commercehub_runtime() {
  local ns="${1:-commercehub}"
  local host="${2:?INFRA_HOST required}"
  kube create configmap commercehub-runtime \
    --namespace "${ns}" \
    --from-literal=DB_URL="jdbc:oracle:thin:@//${host}:1521/FREEPDB1" \
    --from-literal=KAFKA_BOOTSTRAP_SERVERS="${host}:39092" \
    --from-literal=OTEL_EXPORTER_OTLP_ENDPOINT="http://${host}:4317" \
    --dry-run=client -o yaml | kube apply -f -
}

detect_infra_host() {
  if [[ -n "${INFRA_HOST:-}" ]]; then
    printf '%s\n' "${INFRA_HOST}"
    return
  fi
  local host
  host="$(ip -4 route get 1.1.1.1 2>/dev/null | awk '{for (i=1;i<=NF;i++) if ($i=="src") {print $(i+1); exit}}')"
  if [[ -z "${host}" ]]; then
    host="$(hostname -I 2>/dev/null | awk '{print $1}')"
  fi
  if [[ -z "${host}" ]]; then
    echo "Set INFRA_HOST to the IPv4 address OpenShift pods should use to reach Oracle/Kafka/Jaeger on the host." >&2
    exit 1
  fi
  printf '%s\n' "${host}"
}
