#!/usr/bin/env bash
# Idempotent Cloud Agent install: local .env + Maven dependency cache.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [ ! -f .env ]; then
  cp .env.example .env
  # Stable local demo passwords (never commit .env; values are for this VM only).
  sed -i \
    -e 's/change-me-sys/commercehub-sys/' \
    -e 's/change-me-user/commercehub-user/' \
    -e 's/change-me-product/commercehub-product/' \
    -e 's/change-me-order/commercehub-order/' \
    -e 's/change-me-inventory/commercehub-inventory/' \
    .env
fi

./mvnw -B -ntp -DskipTests dependency:go-offline
./mvnw -B -ntp -DskipTests package
