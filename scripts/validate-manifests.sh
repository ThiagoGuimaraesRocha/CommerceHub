#!/usr/bin/env bash
# Checks OpenShift manifests for the fields CI can assert without a cluster.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST_DIR="${ROOT_DIR}/deploy/openshift"

python3 - "${MANIFEST_DIR}" <<'PY'
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
errors = 0
required = [
    "namespace.yaml",
    "configmap.yaml",
    "kustomization.yaml",
]
for name in (
    "user-service",
    "product-service",
    "order-service",
    "inventory-service",
):
    required.extend(
        [
            f"{name}-deployment.yaml",
            f"{name}-service.yaml",
            f"{name}-route.yaml",
            f"{name}-migrate-job.yaml",
        ]
    )

for name in required:
    path = root / name
    if not path.is_file():
        print(f"missing {path}")
        errors += 1
        continue
    text = path.read_text()
    if "\t" in text:
        print(f"{path}: contains tabs")
        errors += 1
    if name == "kustomization.yaml":
        if "kind: Kustomization" not in text:
            print(f"{path}: not a Kustomization")
            errors += 1
        continue
    if "apiVersion:" not in text or "kind:" not in text:
        print(f"{path}: missing apiVersion/kind")
        errors += 1
    if name.endswith("-deployment.yaml"):
        for token in ("/q/health/live", "/q/health/ready"):
            if token not in text:
                print(f"{path}: missing {token}")
                errors += 1
        if "secretKeyRef" not in text:
            print(f"{path}: passwords must come from a Secret")
            errors += 1
        if "emptyDir" not in text:
            print(f"{path}: expected emptyDir for /tmp (no PVC)")
            errors += 1
        if "persistentVolumeClaim" in text or "kind: PersistentVolumeClaim" in text:
            print(f"{path}: PVCs are not used (TopoLVM is out of scope)")
            errors += 1
    if name.endswith("-migrate-job.yaml"):
        if "COMMERCEHUB_MIGRATE_ONLY" not in text:
            print(f"{path}: missing COMMERCEHUB_MIGRATE_ONLY")
            errors += 1
        if 'value: "true"' not in text:
            print(f"{path}: migrate job must enable Flyway")
            errors += 1
        if "emptyDir" not in text:
            print(f"{path}: expected emptyDir for /tmp (no PVC)")
            errors += 1
    if name.endswith("-route.yaml"):
        if "route.openshift.io/v1" not in text:
            print(f"{path}: Route must use route.openshift.io/v1")
            errors += 1
    if name.endswith("-deployment.yaml"):
        if "commercehub-runtime" not in text:
            print(f"{path}: expected commercehub-runtime ConfigMap (host-specific, not in Git)")
            errors += 1
    if name.endswith("-migrate-job.yaml"):
        if "argocd.argoproj.io/hook: PreSync" not in text:
            print(f"{path}: migrate Job should be an Argo CD PreSync hook")
            errors += 1
        if "commercehub-runtime" not in text:
            print(f"{path}: expected commercehub-runtime ConfigMap (host-specific, not in Git)")
            errors += 1
    if name == "configmap.yaml":
        if "__INFRA_HOST__" in text or "DB_URL:" in text:
            print(f"{path}: host-specific DB/Kafka/OTEL belong in commercehub-runtime, not Git")
            errors += 1
        if 'FLYWAY_MIGRATE_AT_START: "false"' not in text:
            print(f"{path}: application pods must set FLYWAY_MIGRATE_AT_START=false")
            errors += 1

argocd = pathlib.Path(root).parent / "argocd" / "application.yaml"
if not argocd.is_file():
    print(f"missing {argocd}")
    errors += 1
else:
    text = argocd.read_text()
    if "kind: Application" not in text or "argoproj.io" not in text:
        print(f"{argocd}: expected Argo CD Application")
        errors += 1
    if "__GHCR_OWNER__" not in text or "__IMAGE_TAG__" not in text:
        print(f"{argocd}: expected image placeholders for install-time substitution")
        errors += 1

if errors:
    sys.exit(1)
print("OpenShift manifests look consistent")
PY

for script in \
  "${ROOT_DIR}/scripts/up.sh" \
  "${ROOT_DIR}/scripts/down.sh" \
  "${ROOT_DIR}/scripts/openshift-apply.sh" \
  "${ROOT_DIR}/scripts/openshift-rollback.sh" \
  "${ROOT_DIR}/scripts/microshift-start.sh" \
  "${ROOT_DIR}/scripts/argocd-install.sh" \
  "${ROOT_DIR}/scripts/validate-manifests.sh"
do
  bash -n "${script}"
done

echo "Script syntax OK"
