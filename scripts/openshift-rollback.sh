#!/usr/bin/env bash
# Rolls back one CommerceHub Deployment to the previous ReplicaSet (RollingUpdate).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/lib/cluster.sh"

NAMESPACE="${NAMESPACE:-commercehub}"
SERVICE="${1:-}"
export KUBECONFIG="${KUBECONFIG:-${HOME}/.kube/commercehub-microshift}"

case "${SERVICE}" in
  user-service|product-service|order-service|inventory-service) ;;
  *)
    echo "Usage: $0 <user-service|product-service|order-service|inventory-service>" >&2
    exit 1
    ;;
esac

echo "Current revision:"
kube rollout history "deployment/${SERVICE}" -n "${NAMESPACE}"
kube rollout undo "deployment/${SERVICE}" -n "${NAMESPACE}"
kube rollout status "deployment/${SERVICE}" -n "${NAMESPACE}" --timeout=180s
echo "Rolled back ${SERVICE}."
echo "Re-check: kube get pods -n ${NAMESPACE} -l app.kubernetes.io/name=${SERVICE}"
