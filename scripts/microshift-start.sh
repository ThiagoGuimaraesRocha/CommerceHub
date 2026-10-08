#!/usr/bin/env bash
# Starts MicroShift upstream (OKD) as a privileged Podman bootc container (ADR 0009).
# Official docs cover Podman, not Docker Engine.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "${ROOT_DIR}/scripts/lib/cluster.sh"

MICROSHIFT_IMAGE="${MICROSHIFT_IMAGE:-ghcr.io/microshift-io/microshift:4.20.0_g153ff0ca9_4.20.0_okd_scos.16}"
MICROSHIFT_NAME="${MICROSHIFT_NAME:-commercehub-microshift}"
KUBECONFIG_OUT="${KUBECONFIG_OUT:-${HOME}/.kube/commercehub-microshift}"

need_cmd podman

if podman container exists "${MICROSHIFT_NAME}" 2>/dev/null; then
  if [[ "$(podman inspect -f '{{.State.Running}}' "${MICROSHIFT_NAME}")" == "true" ]]; then
    echo "MicroShift container ${MICROSHIFT_NAME} is already running."
  else
    podman start "${MICROSHIFT_NAME}"
  fi
else
  echo "Starting MicroShift ${MICROSHIFT_IMAGE} (privileged bootc; this needs Linux + rootful Podman)."
  podman run -d \
    --name "${MICROSHIFT_NAME}" \
    --privileged \
    --systemd=always \
    --hostname microshift \
    -p 6443:6443 \
    -p 80:80 \
    -p 443:443 \
    -v commercehub-microshift-lib:/var/lib \
    "${MICROSHIFT_IMAGE}"
fi

mkdir -p "$(dirname "${KUBECONFIG_OUT}")"
echo "Waiting for kubeconfig inside the container..."
for _ in $(seq 1 90); do
  if podman exec "${MICROSHIFT_NAME}" test -s /var/lib/microshift/resources/kubeadmin/kubeconfig 2>/dev/null; then
    podman cp "${MICROSHIFT_NAME}:/var/lib/microshift/resources/kubeadmin/kubeconfig" "${KUBECONFIG_OUT}"
    # API is published on the host at 6443.
    python3 - "${KUBECONFIG_OUT}" <<'PY'
import pathlib, sys
path = pathlib.Path(sys.argv[1])
text = path.read_text()
text = text.replace("https://microshift:6443", "https://127.0.0.1:6443")
text = text.replace("https://localhost:6443", "https://127.0.0.1:6443")
path.write_text(text)
PY
    export KUBECONFIG="${KUBECONFIG_OUT}"
    echo "KUBECONFIG=${KUBECONFIG_OUT}"
    echo "Waiting for the Kubernetes API..."
    for __ in $(seq 1 60); do
      if kube get --raw=/readyz >/dev/null 2>&1 || kube get ns >/dev/null 2>&1; then
        echo "MicroShift API is ready."
        exit 0
      fi
      sleep 5
    done
    echo "kubeconfig exists but the API did not become ready in time." >&2
    exit 1
  fi
  sleep 5
done

cat >&2 <<EOF
Timed out waiting for MicroShift kubeconfig.
The published bootc image is started with Podman (not Docker). If this host cannot
run a privileged systemd container, start the cluster with the upstream helper
and then run scripts/openshift-apply.sh:

  curl -s https://microshift-io.github.io/microshift/quickstart.sh | \\
    sudo OWNER=microshift-io TAG=4.20.0_g153ff0ca9_4.20.0_okd_scos.16 bash
EOF
exit 1
