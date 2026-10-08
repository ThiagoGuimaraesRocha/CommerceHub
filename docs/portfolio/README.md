# Portfolio capture checklist

CommerceHub is a **backend** platform. The versioned evidence in Git is the Grafana dashboard
JSON, Prometheus scrape config, CI workflow, OpenShift manifests and this checklist. Screenshots
and a short demo video are captured on a Linux workstation with Docker + Podman (this cloud CI
runner cannot start MicroShift).

Do **not** commit secrets, JWT tokens, or `.env` values into screenshots.

## What to capture (after `./scripts/up.sh` or Compose `apps`)

1. **Order flow** — Postman or curl: login, create order, confirm, stock reserved; then a
   customer cancel with a reason.
2. **Jaeger** — `http://localhost:16686`, service `order-service`, one confirm trace that
   includes Inventory.
3. **Grafana** — `http://localhost:3000`, provisioned **CommerceHub** dashboard (folder CommerceHub).
4. **OpenShift** — `oc get pods,routes -n commercehub`. Topology view only exists on CRC or the
   Developer Sandbox (MicroShift has no console); ADR 0009.
5. **CI** — GitHub Actions green run on `main`, and (after the first merge of Sprint 8) GHCR
   packages for the four images.
6. **Argo CD** — Application `commercehub` Synced/Healthy (when `GITOPS=1`).

## Short video (3–5 min)

Create → confirm → `OrderConfirmed` → inventory reserved → order status update → cancel with a
customer reason → open the same `traceparent` in Jaeger and the Grafana counters.

## In this repository

| Artifact | Path |
| --- | --- |
| Grafana dashboard | `infra/grafana/dashboards/commercehub.json` |
| Prometheus scrape | `infra/prometheus/prometheus.yml` |
| OpenShift manifests | `deploy/openshift/` |
| GitOps Application | `deploy/argocd/application.yaml` |
| CI | `.github/workflows/ci.yml` |
| Postman | `postman/` |
