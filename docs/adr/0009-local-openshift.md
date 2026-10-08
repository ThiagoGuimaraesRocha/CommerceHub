# ADR 0009 — Local OpenShift environment

- Status: Accepted (Sprint 7)
- Date: 2026-09-29
- Updated: 2026-10-08

## Context

Sprint 7 must show the services running on OpenShift. The Red Hat Developer Sandbox has quotas and a time
limit, and Oracle + Kafka + observability hardly fit in it. The goal is an OpenShift environment that runs
on the developer machine and starts together with the rest of the local stack.

## Options

| Option | What it is | Runs in a container? | OpenShift APIs | Web console | Host requirements |
| --- | --- | --- | --- | --- | --- |
| **MicroShift upstream (OKD)** — `ghcr.io/microshift-io/microshift` | Single-node OpenShift-derived Kubernetes built from OKD, no pull secret | Yes, as a privileged bootc container (systemd inside) | Routes, SCC, OpenShift security model | No | Linux with Podman (rootful) or Podman machine on macOS/Windows; ~2–4 GB RAM |
| OpenShift Local (CRC) | Full single-node OpenShift | No, it is a VM | All | Yes | Hypervisor, ~10–11 GB RAM for the cluster, free Red Hat pull secret |
| Developer Sandbox | Shared remote cluster | n/a | All | Yes | Red Hat account, quotas, 30-day window |

## Decision

1. **MicroShift upstream (OKD) in a container** is the local OpenShift target. It is the only option that
   can be started together with the Compose stack.
2. Infrastructure that is heavy or stateful (Oracle, Kafka) stays in Docker Compose; the four services are
   deployed **into** MicroShift from the versioned manifests, reaching Oracle and Kafka through the host.
   This keeps memory usage manageable and mirrors a real setup where databases live outside the cluster.
3. A single entry point, `scripts/up.sh`, starts Compose (Oracle, Kafka, observability), starts MicroShift
   with Podman, waits for the API and applies the manifests. There is no Compose `openshift` profile:
   Docker Engine is not a supported MicroShift runtime.
4. OpenShift Local (CRC) or the Developer Sandbox is used only to capture web console screenshots
   (Topology view) for the portfolio, since MicroShift has no console.

## Spike results (Sprint 7)

- MicroShift upstream documents **Podman** (privileged bootc + systemd). Docker Engine is not a supported
  runtime, so there is **no** Compose `openshift` profile. `scripts/up.sh` starts MicroShift with Podman
  and Compose only for Oracle, Kafka and observability.
- Storage: CommerceHub pods use **no PVC** (stateless JVMs). TopoLVM is not required for these workloads.
- Kafka exposes a dedicated `OPENSHIFT` listener on host port `39092` so advertised brokers match `INFRA_HOST`.
- Argo CD (Sprint 8) must be installed from upstream manifests; MicroShift has no OperatorHub by default.
- Pinned image: `ghcr.io/microshift-io/microshift:4.20.0_g153ff0ca9_4.20.0_okd_scos.16`.

## Consequences

- "One command brings everything up" is achievable, but OpenShift cannot be a plain Compose service like
  Oracle: it is a full node (systemd, CRI-O, networking) and needs a privileged container.
- The daily development loop (`quarkus:dev`, Compose) does not depend on the cluster.
