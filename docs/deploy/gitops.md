# GitOps (Argo CD) and GHCR

Sprint 8. Decision: [ADR 0012](../adr/0012-cicd-gitops-ghcr.md).

## Images (GHCR)

Merge to `main` (after tests) publishes:

`ghcr.io/<owner>/commercehub-{user,product,order,inventory}-service`

| Tag | When |
| --- | --- |
| `0.8.0` | application version (every main publish) |
| `sha-<7 hex>` | git commit |
| `vX.Y.Z` | only when a git tag `v*` is pushed |

OCI labels: `org.opencontainers.image.source`, `revision`, `version`. Digests are whatever GHCR
returns; they are not committed.

The `GITHUB_TOKEN` in Actions has `packages: write`. The first package may be **private** even for
a public repo — set package visibility to public in GitHub Packages if you want anonymous pulls.

## GitOps on MicroShift

Oracle, Kafka and observability stay in Compose. Argo CD lives in the cluster and syncs
`deploy/openshift` from `main`.

```bash
export KUBECONFIG="${HOME}/.kube/commercehub-microshift"
GITOPS=1 ./scripts/up.sh
```

Or, with the cluster already up:

```bash
./scripts/argocd-install.sh
```

Pinned install: Argo CD **v3.5.4**
(`https://raw.githubusercontent.com/argoproj/argo-cd/v3.5.4/manifests/install.yaml`).

What the script does:

1. Creates namespace `commercehub`, Secret `commercehub-secrets` (from `.env`) and ConfigMap
   `commercehub-runtime` (`INFRA_HOST` for Oracle/Kafka/Jaeger). Those objects are **not** in Git.
2. Applies the upstream Argo CD install into `argocd`, grants `anyuid` SCCs when `oc` is available,
   and exposes `argocd-server` with an OpenShift Route (TLS passthrough).
3. Applies `deploy/argocd/application.yaml` with `GHCR_OWNER` / `IMAGE_TAG` substituted.

Admin password (never committed, not printed by the script):

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d
```

Flyway Jobs are Argo CD `PreSync` hooks (`BeforeHookCreation`). Application pods keep
`FLYWAY_MIGRATE_AT_START=false`.

Default `./scripts/up.sh` without `GITOPS=1` still applies manifests with `openshift-apply.sh`
(Sprint 7 path). Use that when you do not want Argo CD’s extra RAM.

## Jenkins

Root `Jenkinsfile`: validate manifests, then `./mvnw -B -ntp verify`. The agent needs JDK 21 and
Docker (Dev Services). GitHub Actions remains the primary pipeline.
