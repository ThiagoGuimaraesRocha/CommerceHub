#!/usr/bin/env bash
# Per-boot Cloud Agent start: Docker daemon + Oracle for Quarkus Dev Services / compose.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

ensure_dockerd() {
  if docker info >/dev/null 2>&1; then
    return 0
  fi

  sudo mkdir -p /etc/docker
  if [ ! -f /etc/docker/daemon.json ]; then
    cat <<'EOF' | sudo tee /etc/docker/daemon.json >/dev/null
{
  "storage-driver": "fuse-overlayfs",
  "iptables": false,
  "ip6tables": false
}
EOF
  fi

  if ! pgrep -x dockerd >/dev/null 2>&1; then
    sudo dockerd >/tmp/dockerd.log 2>&1 &
  fi

  for _ in $(seq 1 60); do
    if docker info >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done

  if ! docker info >/dev/null 2>&1; then
    echo "Docker daemon failed to become ready" >&2
    tail -n 50 /tmp/dockerd.log >&2 || true
    exit 1
  fi

  # Nested Cloud Agent VMs often lack a login session for the docker group.
  if [ ! -w /var/run/docker.sock ]; then
    sudo chmod 666 /var/run/docker.sock || true
  fi
}

if [ ! -f .env ]; then
  "${ROOT_DIR}/scripts/cloud-agent-install.sh"
fi

ensure_dockerd

docker compose up -d oracle

echo "Waiting for Oracle to become healthy..."
for _ in $(seq 1 90); do
  status="$(docker compose ps --format json oracle 2>/dev/null | python3 -c 'import sys,json; d=json.loads(sys.stdin.read() or "{}"); print(d.get("Health") or d.get("State") or "")' 2>/dev/null || true)"
  if [ "${status}" = "healthy" ]; then
    echo "Oracle is healthy."
    exit 0
  fi
  # Fallback when compose JSON format differs
  if docker compose ps oracle 2>/dev/null | grep -qi healthy; then
    echo "Oracle is healthy."
    exit 0
  fi
  sleep 2
done

echo "Oracle did not become healthy in time" >&2
docker compose ps oracle >&2 || true
docker compose logs --tail=80 oracle >&2 || true
exit 1
