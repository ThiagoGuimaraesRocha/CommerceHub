# Local OpenShift (MicroShift / OKD)

Sprint 7 deploys the four CommerceHub services **inside** a local OpenShift-compatible cluster.
Oracle, Kafka and the observability stack stay in Docker Compose. Secrets are applied from `.env`
and are never committed.

Decision: [ADR 0009](../adr/0009-local-openshift.md).

## Requirements

- Linux with **rootful Podman** (MicroShift’s documented runtime). macOS/Windows: a Linux VM or Podman
  machine. Docker Engine is **not** used to run MicroShift.
- Docker Engine + Compose v2 for Oracle, Kafka, Jaeger, Prometheus and Grafana.
- `oc` or `kubectl`.
- About 2–4 GB extra RAM for MicroShift, on top of the Compose stack.
- `.env` copied from `.env.example`.

Pinned cluster image: `ghcr.io/microshift-io/microshift:4.20.0_g153ff0ca9_4.20.0_okd_scos.16`
(no invented digest). Application images: `ghcr.io/<owner>/commercehub-*-service:0.8.0`.

Workloads use `emptyDir` only (no PersistentVolumeClaim), so TopoLVM is not required for CommerceHub
pods.

## One command

```bash
cp .env.example .env          # set passwords
./scripts/up.sh
```

`up.sh` starts Compose (without the `apps` profile), builds the four images, starts MicroShift with
Podman, loads the images into the node when possible, creates `commercehub-secrets` from `.env` and
ConfigMap `commercehub-runtime` (`INFRA_HOST`), runs Flyway **Jobs**, then rolling Deployments.

GitOps instead of imperative apply: `GITOPS=1 ./scripts/up.sh` ([guide](gitops.md)).

```bash
export KUBECONFIG="${HOME}/.kube/commercehub-microshift"
oc get pods,routes -n commercehub
PRODUCT=$(oc get route product-service -n commercehub -o jsonpath='{.spec.host}')
INVENTORY=$(oc get route inventory-service -n commercehub -o jsonpath='{.spec.host}')
USER=$(oc get route user-service -n commercehub -o jsonpath='{.spec.host}')
./scripts/seed.sh "http://${PRODUCT}" "http://${INVENTORY}" "http://${USER}"
```

Set `INFRA_HOST` if autodetect is wrong: it is the IPv4 address pods use to reach Oracle (`1521`),
Kafka (`39092`) and Jaeger (`4317`) on the host. `up.sh` also sets Kafka’s `OPENSHIFT` advertised
listener to that address.

## Apply only (cluster already running)

CRC, Developer Sandbox or an existing MicroShift:

```bash
export KUBECONFIG=/path/to/kubeconfig
export INFRA_HOST=192.168.1.10          # host IP reachable from the cluster
./scripts/openshift-apply.sh
```

## Rollback

Deployments use `RollingUpdate` (`maxUnavailable: 0`), so the previous ReplicaSet is kept.

```bash
./scripts/openshift-rollback.sh order-service
```

That is `oc rollout undo deployment/order-service -n commercehub` plus `rollout status`.
Verified with `bash -n` and by inspecting the Deployment strategy; a live undo needs a running
cluster.

To roll forward again, re-apply the same manifests (`./scripts/openshift-apply.sh`) or
`oc rollout undo` a second time.

## Probes and migrations

| Setting | Value |
| --- | --- |
| Liveness | `GET /q/health/live` |
| Readiness | `GET /q/health/ready` (includes datasource and outbox) |
| Jobs | `FLYWAY_MIGRATE_AT_START=true` and `COMMERCEHUB_MIGRATE_ONLY=true` (process exits after Flyway) |
| Pods | `FLYWAY_MIGRATE_AT_START=false` (ConfigMap) |

## Stop

```bash
./scripts/down.sh                 # namespace + Compose; keeps Oracle and MicroShift volumes
MICROSHIFT_DELETE=1 ./scripts/down.sh
```

## If MicroShift does not become Ready

The bootc image is a full node (systemd, CRI-O). If the simplified `podman run --privileged --systemd=always`
path fails on your kernel, use the upstream helper and then `scripts/openshift-apply.sh`:

```bash
curl -s https://microshift-io.github.io/microshift/quickstart.sh | \
  sudo OWNER=microshift-io TAG=4.20.0_g153ff0ca9_4.20.0_okd_scos.16 bash
```

Web console screenshots (Topology) are captured with OpenShift Local (CRC) or the Developer Sandbox;
MicroShift has no console.
