#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMAGE="cfc-lab-management-backend:office"
CONTAINER="cfc-lab-backend-office"
M2_CACHE="$(getent passwd "$(id -u)" | cut -d: -f6)/.m2"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker chưa được cài hoặc chưa có trong PATH." >&2
  exit 1
fi

if [[ ! -f "$ROOT/.env" ]]; then
  echo "Không tìm thấy .env tại $ROOT/.env." >&2
  exit 1
fi

# Read the local env file as shell assignments so values such as DB_URL="..."
# lose their quoting. Docker's --env-file keeps those quotes literally.
set -a
# shellcheck disable=SC1091
. "$ROOT/.env"
set +a

echo "Đang build backend image có LibreOffice: $IMAGE"
docker build -f "$ROOT/source/be/Dockerfile" -t "$IMAGE" "$ROOT/source/be"

# Chỉ thay container chạy backend của project này; không đụng MySQL/Redis hay container khác.
docker rm -f "$CONTAINER" >/dev/null 2>&1 || true

echo "Backend sẽ chạy tại http://localhost:8080"
echo "DOCX -> PDF dùng soffice trong container. Nhấn Ctrl+C để dừng."

exec docker run --rm \
  --name "$CONTAINER" \
  --network host \
  -e SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-mysql}" \
  -e ADMIN_EMAIL="${ADMIN_EMAIL:-}" \
  -e ADMIN_PASSWORD="${ADMIN_PASSWORD:-}" \
  -e JWT_SECRET="${JWT_SECRET:-}" \
  -e FRONTEND_ORIGIN="${FRONTEND_ORIGIN:-http://localhost:5173}" \
  -e SECURE_COOKIE="${SECURE_COOKIE:-false}" \
  -e DB_URL="${DB_URL:-jdbc:mysql://127.0.0.1:3306/cfc_lab?useSSL=false&serverTimezone=UTC}" \
  -e DB_USERNAME="${DB_USERNAME:-}" \
  -e DB_PASSWORD="${DB_PASSWORD:-}" \
  -e REDIS_HOST="${REDIS_HOST:-127.0.0.1}" \
  -e REDIS_PORT="${REDIS_PORT:-6379}" \
  -e REDIS_DATABASE="${REDIS_DATABASE:-0}" \
  -e REDIS_PASSWORD="${REDIS_PASSWORD:-}" \
  -e PUBLIC_LOOKUP_URL="${PUBLIC_LOOKUP_URL:-http://192.168.1.132:5173/tracuu/}" \
  -e PUBLIC_CLIENT_IP_HEADER="${PUBLIC_CLIENT_IP_HEADER:-}" \
  -e REPORT_OFFICE_COMMAND=soffice \
  -v "$ROOT/source/be:/app" \
  -v "$M2_CACHE:/root/.m2" \
  -w /app \
  "$IMAGE" mvn -B spring-boot:run

