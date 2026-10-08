#!/usr/bin/env bash
# Tears down OpenShift apps (optional) and the Compose infrastructure.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/lib/cluster.sh"

cd "${ROOT_DIR}"
NAMESPACE="${NAMESPACE:-commercehub}"
MICROSHIFT_NAME="${MICROSHIFT_NAME:-commercehub-microshift}"
export KUBECONFIG="${KUBECONFIG:-${HOME}/.kube/commercehub-microshift}"

if command -v oc >/dev/null 2>&1 || command -v kubectl >/dev/null 2>&1; then
  if [[ -f "${KUBECONFIG}" ]]; then
    echo "Deleting namespace ${NAMESPACE} (ignore errors if the cluster is already down)..."
    kube delete namespace "${NAMESPACE}" --ignore-not-found --wait=false || true
  fi
fi

if command -v podman >/dev/null 2>&1 && podman container exists "${MICROSHIFT_NAME}" 2>/dev/null; then
  echo "Stopping MicroShift container ${MICROSHIFT_NAME}..."
  podman stop "${MICROSHIFT_NAME}" || true
  if [[ "${MICROSHIFT_DELETE:-}" == "1" ]]; then
    podman rm "${MICROSHIFT_NAME}" || true
    podman volume rm commercehub-microshift-lib || true
  fi
fi

echo "Stopping Compose..."
docker compose --profile apps down
echo "Done. Oracle volume is kept; use docker compose down -v to delete it."
echo "To also remove the MicroShift container and volume: MICROSHIFT_DELETE=1 ./scripts/down.sh"
