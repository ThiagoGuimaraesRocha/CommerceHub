#!/usr/bin/env bash
# Loads demo data through the public REST APIs (never directly into the database).
# Safe to run more than once: records that already exist (HTTP 409) are skipped.
# Usage: ./scripts/seed.sh [product-service-url]

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PRODUCT_URL="${1:-${PRODUCT_SERVICE_URL:-http://localhost:8082}}"

created=0
skipped=0

while IFS= read -r line || [ -n "${line}" ]; do
  [ -z "${line}" ] && continue
  status=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "${PRODUCT_URL}/api/v1/products" \
    -H 'Content-Type: application/json' \
    -d "${line}")
  case "${status}" in
    201) created=$((created + 1)) ;;
    409) skipped=$((skipped + 1)) ;;
    *) echo "Failed to create product (HTTP ${status}): ${line}" >&2; exit 1 ;;
  esac
done < "${ROOT_DIR}/data/seed/products.ndjson"

echo "Products: ${created} created, ${skipped} already existed."
