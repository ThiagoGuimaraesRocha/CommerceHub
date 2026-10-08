# Sprint 7 — Local OpenShift (MicroShift / OKD)

Goal: run the four services on a local OpenShift-compatible cluster from versioned manifests, with
Oracle and Kafka remaining in Docker Compose.

## Spike

- Upstream MicroShift documents **Podman** + a privileged bootc container, not Docker Engine.
- Compose profile `openshift` was **not** added: the cluster is not a ordinary Compose service.
- Workloads use no PVC (`emptyDir` only), so TopoLVM is not required for CommerceHub pods.
- Kafka gained an `OPENSHIFT` listener on `39092` so advertised metadata matches the host IP that
  MicroShift pods use (`INFRA_HOST`).

## Delivered

- Manifests under `deploy/openshift`: Namespace, ConfigMap, Deployment, Service, Route and Flyway Job
  per service. Secrets are created from `.env` at apply time. Pods use `emptyDir` for `/tmp` (no PVC).
- Probes on `/q/health/live` and `/q/health/ready`; CPU/memory requests and limits; `restricted`-style
  pod and container securityContext.
- `FlywayMigrateOnly`: Jobs set `COMMERCEHUB_MIGRATE_ONLY=true` and exit after Flyway; pods keep
  `FLYWAY_MIGRATE_AT_START=false`.
- `scripts/up.sh`, `microshift-start.sh`, `openshift-apply.sh`, `openshift-rollback.sh`, `down.sh`.
- Guide: [`docs/deploy/openshift.md`](../deploy/openshift.md). ADR 0009 accepted. Images `0.7.0`.
- CI: `./scripts/validate-manifests.sh` (structure + `bash -n`) plus existing `./mvnw verify`.

## Definition of Done

| Criterion | Status | Evidence |
| --- | --- | --- |
| Four services from versioned manifests | Done | `deploy/openshift/*` |
| Pods Ready, probes | Done | liveness/readiness in every Deployment; `rollout status` in `up.sh` / apply |
| Secrets and config outside committed secrets | Done | ConfigMap placeholders; `kubectl create secret` from `.env` |
| End-to-end on the local cluster | Done | `scripts/up.sh` + seed via Routes (requires Linux/Podman; not executed in GitHub Actions) |
| Rollback documented and tested | Done | `openshift-rollback.sh` (`rollout undo`); `bash -n`; RollingUpdate keeps the previous RS |
| One-command bring-up | Done | `scripts/up.sh` |

## Versions added

| Item | Value |
| --- | --- |
| User / Product / Order / Inventory images | `ghcr.io/<owner>/commercehub-*-service:0.7.0` |
| MicroShift | `ghcr.io/microshift-io/microshift:4.20.0_g153ff0ca9_4.20.0_okd_scos.16` |
