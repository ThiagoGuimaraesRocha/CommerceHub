#!/usr/bin/env bash
# Loads demo data through the public REST APIs (never directly into the database).
# Safe to run more than once: products that already exist (HTTP 409) are skipped;
# inventory PUT is idempotent (absolute available quantity); users skip on 409.
# Usage: ./scripts/seed.sh [product-service-url] [inventory-service-url] [user-service-url]
# OpenShift: pass Route URLs, or set PRODUCT_SERVICE_URL / INVENTORY_SERVICE_URL / USER_SERVICE_URL.
#
# Requires DEMO_ADMIN_EMAIL and DEMO_ADMIN_PASSWORD (see .env.example). The User
# Service must already have created the bootstrap admin. This script never prints
# tokens or passwords.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ -f "${ROOT_DIR}/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

PRODUCT_URL="${1:-${PRODUCT_SERVICE_URL:-http://localhost:8082}}"
INVENTORY_URL="${2:-${INVENTORY_SERVICE_URL:-http://localhost:8084}}"
USER_URL="${3:-${USER_SERVICE_URL:-http://localhost:8081}}"

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to resolve productId from SKU and parse login." >&2
  exit 1
fi

if [ -z "${DEMO_ADMIN_EMAIL:-}" ] || [ -z "${DEMO_ADMIN_PASSWORD:-}" ]; then
  echo "DEMO_ADMIN_EMAIL and DEMO_ADMIN_PASSWORD are required. Copy .env.example to .env." >&2
  exit 1
fi

login_body=$(python3 -c 'import json,os; print(json.dumps({"email":os.environ["DEMO_ADMIN_EMAIL"],"password":os.environ["DEMO_ADMIN_PASSWORD"]}))')
login_response=$(curl -sS -X POST "${USER_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "${login_body}")
token=$(python3 -c '
import json,sys
body = json.loads(sys.argv[1])
token = body.get("accessToken")
if not token:
    sys.stderr.write("Login failed. Is the User Service up and was the bootstrap admin created?\n")
    sys.exit(2)
print(token)
' "${login_response}")
AUTH=("Authorization: Bearer ${token}")

users_created=0
users_skipped=0
if [ -f "${ROOT_DIR}/data/seed/users.ndjson" ]; then
  while IFS= read -r line || [ -n "${line}" ]; do
    [ -z "${line}" ] && continue
    status=$(curl -s -o /dev/null -w '%{http_code}' \
      -X POST "${USER_URL}/api/v1/users" \
      -H 'Content-Type: application/json' \
      -H "${AUTH[0]}" \
      -d "${line}")
    case "${status}" in
      201) users_created=$((users_created + 1)) ;;
      409) users_skipped=$((users_skipped + 1)) ;;
      *) echo "Failed to create user (HTTP ${status})" >&2; exit 1 ;;
    esac
  done < "${ROOT_DIR}/data/seed/users.ndjson"
fi
echo "Users: ${users_created} created, ${users_skipped} already existed."

created=0
skipped=0

while IFS= read -r line || [ -n "${line}" ]; do
  [ -z "${line}" ] && continue
  status=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "${PRODUCT_URL}/api/v1/products" \
    -H 'Content-Type: application/json' \
    -H "${AUTH[0]}" \
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
    -H "${AUTH[0]}" \
    -d "{\"availableQuantity\":${qty}}")
  case "${status}" in
    200) stocked=$((stocked + 1)) ;;
    *) echo "Failed to set stock for product (HTTP ${status})" >&2; exit 1 ;;
  esac
done < "${ROOT_DIR}/data/seed/inventory.ndjson"

echo "Inventory: ${stocked} products stocked via the administrative API."
