#!/usr/bin/env bash
# Applies CommerceHub manifests to the current kube context. Secrets are created
# from .env and are never stored in Git.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/lib/cluster.sh"

load_env_file "${ROOT_DIR}"
need_cmd python3

NAMESPACE="${NAMESPACE:-commercehub}"
GHCR_OWNER="${GHCR_OWNER:-local}"
IMAGE_TAG="${IMAGE_TAG:-0.7.0}"
INFRA_HOST="$(detect_infra_host)"
JWT_ISSUER="${JWT_ISSUER:-https://commercehub.example/issuer}"
RENDERED="$(mktemp -d)"
trap 'rm -rf "${RENDERED}"' EXIT

: "${USER_DB_PASSWORD:?set USER_DB_PASSWORD in .env}"
: "${PRODUCT_DB_PASSWORD:?set PRODUCT_DB_PASSWORD in .env}"
: "${ORDER_DB_PASSWORD:?set ORDER_DB_PASSWORD in .env}"
: "${INVENTORY_DB_PASSWORD:?set INVENTORY_DB_PASSWORD in .env}"
: "${DEMO_ADMIN_EMAIL:?set DEMO_ADMIN_EMAIL in .env}"
: "${DEMO_ADMIN_PASSWORD:?set DEMO_ADMIN_PASSWORD in .env}"
DEMO_ADMIN_FULL_NAME="${DEMO_ADMIN_FULL_NAME:-CommerceHub Admin}"

export KUBECONFIG="${KUBECONFIG:-${HOME}/.kube/commercehub-microshift}"

echo "Using INFRA_HOST=${INFRA_HOST}  namespace=${NAMESPACE}  images=ghcr.io/${GHCR_OWNER}/commercehub-*-service:${IMAGE_TAG}"

python3 - "${ROOT_DIR}/deploy/openshift" "${RENDERED}" <<PY
import pathlib, shutil, sys
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
host = "${INFRA_HOST}"
issuer = "${JWT_ISSUER}"
owner = "${GHCR_OWNER}"
tag = "${IMAGE_TAG}"
for path in src.iterdir():
    text = path.read_text()
    text = text.replace("__INFRA_HOST__", host)
    text = text.replace("https://commercehub.example/issuer", issuer)
    text = text.replace("ghcr.io/local/", f"ghcr.io/{owner}/")
    text = text.replace(":0.7.0", f":{tag}")
    (dst / path.name).write_text(text)
PY

kube apply -f "${RENDERED}/namespace.yaml"
kube create secret generic commercehub-secrets \
  --namespace "${NAMESPACE}" \
  --from-literal=USER_DB_PASSWORD="${USER_DB_PASSWORD}" \
  --from-literal=PRODUCT_DB_PASSWORD="${PRODUCT_DB_PASSWORD}" \
  --from-literal=ORDER_DB_PASSWORD="${ORDER_DB_PASSWORD}" \
  --from-literal=INVENTORY_DB_PASSWORD="${INVENTORY_DB_PASSWORD}" \
  --from-literal=DEMO_ADMIN_EMAIL="${DEMO_ADMIN_EMAIL}" \
  --from-literal=DEMO_ADMIN_PASSWORD="${DEMO_ADMIN_PASSWORD}" \
  --from-literal=DEMO_ADMIN_FULL_NAME="${DEMO_ADMIN_FULL_NAME}" \
  --dry-run=client -o yaml | kube apply -f -

kube apply -f "${RENDERED}/configmap.yaml"

echo "Running Flyway migrate Jobs (FLYWAY_MIGRATE_AT_START=true, pods stay false)..."
kube delete job -n "${NAMESPACE}" -l app.kubernetes.io/component=migrate --ignore-not-found
for svc in user-service product-service order-service inventory-service; do
  kube apply -f "${RENDERED}/${svc}-migrate-job.yaml"
done
for svc in user-service product-service order-service inventory-service; do
  kube wait --for=condition=complete "job/${svc}-migrate" -n "${NAMESPACE}" --timeout=300s
done

echo "Applying Deployments, Services and Routes..."
for svc in user-service product-service order-service inventory-service; do
  kube apply -f "${RENDERED}/${svc}-deployment.yaml"
  kube apply -f "${RENDERED}/${svc}-service.yaml"
  kube apply -f "${RENDERED}/${svc}-route.yaml"
done

for svc in user-service product-service order-service inventory-service; do
  kube rollout status "deployment/${svc}" -n "${NAMESPACE}" --timeout=180s
done

echo
echo "Routes:"
kube get routes -n "${NAMESPACE}" || true
echo
echo "Pods:"
kube get pods -n "${NAMESPACE}"
echo
echo "Rollback a service with: ./scripts/openshift-rollback.sh <service>"
echo "Seed through a Route, for example:"
echo "  ./scripts/seed.sh http://<product-route> http://<inventory-route>"
