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
