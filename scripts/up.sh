#!/usr/bin/env bash
# One command local stack: Docker Compose (Oracle, Kafka, Jaeger, Prometheus, Grafana)
# plus MicroShift and the four services from versioned manifests (Sprint 7 / ADR 0009).
# GITOPS=1 installs Argo CD and syncs from Git instead of kubectl apply (Sprint 8 / ADR 0012).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/lib/cluster.sh"

cd "${ROOT_DIR}"
load_env_file "${ROOT_DIR}"

if [[ ! -f "${ROOT_DIR}/.env" ]]; then
  echo "Missing .env. Run: cp .env.example .env" >&2
  exit 1
fi

INFRA_HOST="$(detect_infra_host)"
export INFRA_HOST
export KAFKA_OPENSHIFT_ADVERTISED_HOST="${KAFKA_OPENSHIFT_ADVERTISED_HOST:-${INFRA_HOST}}"
export GHCR_OWNER="${GHCR_OWNER:-local}"
IMAGE_TAG="${IMAGE_TAG:-0.8.0}"
MICROSHIFT_NAME="${MICROSHIFT_NAME:-commercehub-microshift}"

echo "==> Compose: Oracle + Kafka + observability (no apps profile)"
docker compose up -d --wait

echo "==> Building service images ${IMAGE_TAG}"
docker compose --profile apps build

echo "==> MicroShift (Podman)"
"${ROOT_DIR}/scripts/microshift-start.sh"
export KUBECONFIG="${KUBECONFIG:-${HOME}/.kube/commercehub-microshift}"

load_images_into_microshift() {
  local svc image
  for svc in user-service product-service order-service inventory-service; do
    image="ghcr.io/${GHCR_OWNER}/commercehub-${svc}:${IMAGE_TAG}"
    echo "Loading ${image} into MicroShift..."
    if docker save "${image}" | podman exec -i "${MICROSHIFT_NAME}" podman load >/dev/null 2>&1; then
      continue
    fi
    if docker save "${image}" | podman exec -i "${MICROSHIFT_NAME}" crictl load - >/dev/null 2>&1; then
      continue
    fi
    echo "Could not import ${image} into the MicroShift node; set imagePullPolicy or push to a registry." >&2
  done
}

if command -v podman >/dev/null 2>&1 && podman container exists "${MICROSHIFT_NAME}" 2>/dev/null; then
  load_images_into_microshift
fi

if [[ "${GITOPS:-}" == "1" ]]; then
  echo "==> GitOps: secrets, runtime ConfigMap, Argo CD"
  "${ROOT_DIR}/scripts/argocd-install.sh"
else
  echo "==> Apply manifests"
  "${ROOT_DIR}/scripts/openshift-apply.sh"
fi

echo
echo "Stack is up."
echo "  Oracle/Kafka/Jaeger/Prometheus/Grafana: docker compose ps"
echo "  OpenShift apps: kube get pods -n commercehub  (KUBECONFIG=${KUBECONFIG})"
echo "  Stop: ./scripts/down.sh"
