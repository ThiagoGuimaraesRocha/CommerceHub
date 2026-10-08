#!/usr/bin/env bash
# Loads demo data through the public REST APIs (never directly into the database).
# Safe to run more than once: products that already exist (HTTP 409) are skipped;
# inventory PUT is idempotent (absolute available quantity).
# Usage: ./scripts/seed.sh [product-service-url] [inventory-service-url]

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PRODUCT_URL="${1:-${PRODUCT_SERVICE_URL:-http://localhost:8082}}"
INVENTORY_URL="${2:-${INVENTORY_SERVICE_URL:-http://localhost:8084}}"

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to resolve productId from SKU." >&2
  exit 1
fi

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

products_json=$(curl -fsS "${PRODUCT_URL}/api/v1/products?size=100&active=true")
stocked=0

while IFS= read -r line || [ -n "${line}" ]; do
  [ -z "${line}" ] && continue
  product_id=$(PRODUCTS_JSON="${products_json}" python3 -c '
import json, os, sys
sku = json.loads(sys.argv[1])["sku"]
items = json.loads(os.environ["PRODUCTS_JSON"])["items"]
matches = [p["id"] for p in items if p.get("sku") == sku]
if not matches:
    sys.stderr.write("No product with SKU %s. Seed products first.\n" % sku)
    sys.exit(2)
print(matches[0])
' "${line}")
  qty=$(python3 -c 'import json,sys; print(json.loads(sys.argv[1])["availableQuantity"])' "${line}")
  status=$(curl -s -o /dev/null -w '%{http_code}' \
    -X PUT "${INVENTORY_URL}/api/v1/inventory/${product_id}" \
    -H 'Content-Type: application/json' \
    -d "{\"availableQuantity\":${qty}}")
  case "${status}" in
    200) stocked=$((stocked + 1)) ;;
    *) echo "Failed to set stock for ${product_id} (HTTP ${status}): ${line}" >&2; exit 1 ;;
  esac
done < "${ROOT_DIR}/data/seed/inventory.ndjson"

echo "Inventory: ${stocked} products stocked via the administrative API."
