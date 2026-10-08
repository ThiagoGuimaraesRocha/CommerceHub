#!/usr/bin/env bash
# Installs Argo CD (pinned upstream manifests) on the current kube context and
# applies the CommerceHub Application. Cluster-local secrets/runtime ConfigMap
# are created first and are never stored in Git (Sprint 8 / ADR 0012).
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/lib/cluster.sh"

load_env_file "${ROOT_DIR}"
need_cmd python3

ARGOCD_VERSION="${ARGOCD_VERSION:-v3.5.4}"
ARGOCD_NS="${ARGOCD_NS:-argocd}"
NAMESPACE="${NAMESPACE:-commercehub}"
GHCR_OWNER="${GHCR_OWNER:-thiagoguimaraesrocha}"
IMAGE_TAG="${IMAGE_TAG:-0.8.0}"
INFRA_HOST="$(detect_infra_host)"
export KUBECONFIG="${KUBECONFIG:-${HOME}/.kube/commercehub-microshift}"

INSTALL_URL="https://raw.githubusercontent.com/argoproj/argo-cd/${ARGOCD_VERSION}/manifests/install.yaml"

echo "Argo CD ${ARGOCD_VERSION}"
echo "  install: ${INSTALL_URL}"
echo "  images:  ghcr.io/${GHCR_OWNER}/commercehub-*-service:${IMAGE_TAG}"
echo "  INFRA_HOST=${INFRA_HOST}"

kube create namespace "${NAMESPACE}" --dry-run=client -o yaml | kube apply -f -
apply_commercehub_secrets "${NAMESPACE}"
apply_commercehub_runtime "${NAMESPACE}" "${INFRA_HOST}"

kube create namespace "${ARGOCD_NS}" --dry-run=client -o yaml | kube apply -f -
echo "Applying upstream install manifest (this can take a minute)..."
kube apply -n "${ARGOCD_NS}" --server-side --force-conflicts -f "${INSTALL_URL}"

if command -v oc >/dev/null 2>&1; then
  for sa in argocd-server argocd-application-controller argocd-repo-server argocd-redis argocd-dex-server argocd-notifications-controller argocd-applicationset-controller; do
    oc adm policy add-scc-to-user anyuid -z "${sa}" -n "${ARGOCD_NS}" || true
  done
fi

echo "Waiting for Argo CD CRDs..."
for _ in $(seq 1 60); do
  if kube get crd applications.argoproj.io >/dev/null 2>&1; then
    break
  fi
  sleep 5
done
kube wait --for=condition=established crd/applications.argoproj.io --timeout=180s

kube -n "${ARGOCD_NS}" rollout status deployment/argocd-server --timeout=300s || true

kube apply -f "${ROOT_DIR}/deploy/argocd/route.yaml"

RENDERED="$(mktemp)"
trap 'rm -f "${RENDERED}"' EXIT
python3 - "${ROOT_DIR}/deploy/argocd/application.yaml" "${RENDERED}" <<PY
from pathlib import Path
import sys
text = Path(sys.argv[1]).read_text()
text = text.replace("__GHCR_OWNER__", "${GHCR_OWNER}")
text = text.replace("__IMAGE_TAG__", "${IMAGE_TAG}")
Path(sys.argv[2]).write_text(text)
PY
kube apply -f "${RENDERED}"

echo
echo "Argo CD is installing/syncing."
echo "  UI Route: kube get route argocd-server -n ${ARGOCD_NS}"
echo "  Admin password (not printed here):"
echo "    kube -n ${ARGOCD_NS} get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
echo "  Application: kube -n ${ARGOCD_NS} get application commercehub"
echo "  Workloads:   kube get pods,jobs,routes -n ${NAMESPACE}"
