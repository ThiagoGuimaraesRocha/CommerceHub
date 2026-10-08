# ADR 0012 — CI/CD, GHCR and GitOps

- Status: Accepted (Sprint 8)
- Date: 2026-10-08

## Context

Sprint 8 must show a repeatable path from a green pull request to versioned images and a GitOps
sync on the local OpenShift cluster, without putting secrets in Git.

## Decision

1. **GitHub Actions is the primary pipeline.** `ci.yml` already runs `./scripts/validate-manifests.sh`
   and `./mvnw verify` on every push and pull request. A `publish` job runs only after that job
   succeeds on `main` or on tags `v*`, and pushes the four service images to GHCR.
2. **Image tags are pinned.** Each publish writes `0.8.0` (application version), `sha-<7 chars>`,
   and the git tag when present. `latest` is never used. OCI labels `source`, `revision` and
   `version` are set at build time. Digests are produced by GHCR and are not invented in the repo.
3. **Argo CD is installed from the upstream install manifest** pinned to `v3.5.4`
   (`https://raw.githubusercontent.com/argoproj/argo-cd/v3.5.4/manifests/install.yaml`). MicroShift
   has no OperatorHub. The Application CR lives in `deploy/argocd/application.yaml` and syncs
   `deploy/openshift` from `main`.
4. **Cluster-local config stays out of Git.** Passwords are a Secret from `.env`. Host-specific
   `DB_URL` / Kafka / OTEL endpoints are ConfigMap `commercehub-runtime`, created by
   `scripts/openshift-apply.sh` / `scripts/argocd-install.sh`. Flyway Jobs are Argo CD `PreSync`
   hooks so migrations run before a GitOps sync.
5. **Jenkinsfile is a demonstration alternative** of the same verify path. It is not the system of
   record; GitHub Actions is.

## Consequences

- The first image publish happens on the merge to `main` that introduces this workflow. Until then,
  local `scripts/up.sh` still builds `ghcr.io/local/commercehub-*-service:0.8.0`.
- GHCR packages for a public repository may still be created private; making them public is a
  one-time GitHub UI/settings step, not something this repo can do from YAML.
- Argo CD and MicroShift together are heavy. `GITOPS=1 ./scripts/up.sh` is the GitOps path;
  the default `./scripts/up.sh` still applies manifests imperatively.
