#!/bin/bash
# Runs once, on the first initialization of the Oracle container.
# Creates one schema (database user) per microservice inside FREEPDB1.
# Passwords are injected by docker compose from the local .env file.

set -u

create_schema() {
  local schema="$1"
  local password="$2"

  if [ -z "${password}" ]; then
    echo "ERROR: missing password for ${schema}. Check your .env file." >&2
    return 1
  fi

  echo "Creating schema ${schema} in FREEPDB1"
  createAppUser "${schema}" "${password}" FREEPDB1
}

create_schema USER_SCHEMA      "${USER_DB_PASSWORD:-}"      || exit 1
create_schema PRODUCT_SCHEMA   "${PRODUCT_DB_PASSWORD:-}"   || exit 1
create_schema ORDER_SCHEMA     "${ORDER_DB_PASSWORD:-}"     || exit 1
create_schema INVENTORY_SCHEMA "${INVENTORY_DB_PASSWORD:-}" || exit 1
