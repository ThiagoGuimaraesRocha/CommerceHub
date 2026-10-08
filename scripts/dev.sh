#!/usr/bin/env bash
# Runs one service in Quarkus dev mode with the variables from the root .env file.
# Usage: ./scripts/dev.sh product-service

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE="${1:-}"

if [ -z "${SERVICE}" ] || [ ! -d "${ROOT_DIR}/services/${SERVICE}" ]; then
  echo "Usage: $0 <user-service|product-service|order-service|inventory-service>" >&2
  exit 1
fi

if [ ! -f "${ROOT_DIR}/.env" ]; then
  echo "Missing .env. Run: cp .env.example .env" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source "${ROOT_DIR}/.env"
set +a

export DB_URL="${DB_URL:-jdbc:oracle:thin:@//localhost:${ORACLE_HOST_PORT:-1521}/FREEPDB1}"
export KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS:-localhost:${KAFKA_HOST_PORT:-29092}}"

cd "${ROOT_DIR}"
exec ./mvnw -pl "services/${SERVICE}" -am quarkus:dev
