# Sprint 8 — CI/CD, GitOps and portfolio polish

Goal: publish versioned images to GHCR from green `main`, sync the cluster with Argo CD, keep a
Jenkinsfile as an alternative pipeline, and finish the README/plan.

## Delivered

- GitHub Actions `publish` job (after `verify`, on `main` and tags `v*`): four images to GHCR with
  tags `0.8.0`, `sha-<7>` and the git tag; OCI source/revision/version labels. Never `latest`.
- Argo CD **v3.5.4** from the official install manifest. Application CR in `deploy/argocd`.
  `GITOPS=1 ./scripts/up.sh` or `./scripts/argocd-install.sh`.
- Host-specific ConfigMap `commercehub-runtime` and Secret stay cluster-local. Flyway Jobs are
  Argo CD PreSync hooks.
- `Jenkinsfile`: the same validate + `./mvnw verify` path.
- README, ADR 0012, plan v1.0, capture guide in [`docs/portfolio`](../portfolio/README.md).
  Images `0.8.0`.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| Push/PR runs build and tests | Done | `.github/workflows/ci.yml` since S2 |
| Merge to `main` publishes versioned GHCR images with OCI metadata | Done | `publish` job; labels in Dockerfile + build-push-action |
| Argo CD syncs the cluster from Git | Done | `deploy/argocd/application.yaml`, `scripts/argocd-install.sh` (needs Linux/Podman) |
| Jenkinsfile reproduces the main build/test | Done | root `Jenkinsfile` |
| README explains problem, architecture, stack, run, observability | Done | README + OpenShift + GitOps sections |
| Visual evidence for the portfolio | Done | versioned Grafana dashboard, CI badge, [`docs/portfolio`](../portfolio/README.md) capture checklist. Live Topology/Jaeger screenshots need a local stack (same constraint as S7 e2e). |

## Versions added

| Item | Value |
| --- | --- |
| User / Product / Order / Inventory images | `ghcr.io/<owner>/commercehub-*-service:0.8.0` |
| Argo CD | `v3.5.4` (upstream `manifests/install.yaml`) |
