#!/usr/bin/env bash
# CFC Lab trên máy Linux.
#
#   ./scripts/run-all.sh          # chạy: Ứng dụng (5173), Tra cứu (5173/tracuu/), API (8080)
#   ./scripts/run-all.sh stop     # tắt đúng 3 cái trên
#   ./scripts/run-all.sh status   # kiểm tra kết nối MySQL, Redis, LibreOffice, backend, frontend
#   ./scripts/run-all.sh logs     # xem log backend + frontend
#
# MySQL, Redis: script CHỈ kiểm tra kết nối theo .env và báo lỗi, không bật/tắt/tạo gì.
# Backend chạy trong container có sẵn LibreOffice; frontend chạy bằng nginx. Database tự cập nhật bằng Flyway.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="$ROOT/.env"
BACKEND="cfc-lab-prod-backend"

ok()   { printf '  \033[1;32m✔\033[0m %s\n' "$*"; }
bad()  { printf '  \033[1;31m✘\033[0m %s\n' "$*"; }
say()  { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m[x]\033[0m %s\n' "$*" >&2; exit 1; }
compose() { docker compose -f "$ROOT/docker-compose.yml" "$@"; }

# Đọc một biến từ .env (bỏ dấu ngoặc kép), không in giá trị ra màn hình.
env_get() {
  [[ -f "$ENV_FILE" ]] || return 0
  local line; line="$(grep -E "^[[:space:]]*$1=" "$ENV_FILE" | tail -n 1 || true)"
  line="${line#*=}"; line="${line%\"}"; line="${line#\"}"; line="${line%\'}"; line="${line#\'}"
  printf '%s' "$line"
}

tcp_open() { timeout 3 bash -c "exec 3<>/dev/tcp/$1/$2" 2>/dev/null; }

check_mysql() {
  local url host port
  url="$(env_get DB_URL)"; url="${url:-jdbc:mysql://127.0.0.1:3306/cfc_lab}"
  host="$(sed -E 's#^jdbc:mysql://([^:/?]+).*#\1#' <<<"$url")"
  port="$(sed -nE 's#^jdbc:mysql://[^:/?]+:([0-9]+).*#\1#p' <<<"$url")"; port="${port:-3306}"
  if tcp_open "$host" "$port"; then ok "MySQL     $host:$port"; else bad "MySQL     không kết nối được $host:$port (hãy bật MySQL)"; return 1; fi
}

check_redis() {
  local host port pass reply
  host="$(env_get REDIS_HOST)"; host="${host:-127.0.0.1}"
  port="$(env_get REDIS_PORT)"; port="${port:-6379}"
  pass="$(env_get REDIS_PASSWORD)"
  reply="$(timeout 3 bash -c '
    exec 3<>/dev/tcp/$0/$1 || exit 1
    if [[ -n "$2" ]]; then printf "AUTH %s\r\n" "$2" >&3; read -r a <&3; fi
    printf "PING\r\n" >&3; read -r b <&3; printf "%s|%s" "${a:-}" "$b"' "$host" "$port" "$pass" 2>/dev/null || true)"
  if [[ "$reply" == *"+PONG"* ]]; then ok "Redis     $host:$port"
  elif [[ -z "$reply" ]]; then bad "Redis     không kết nối được $host:$port (hãy bật Redis)"; return 1
  else bad "Redis     $host:$port có chạy nhưng sai mật khẩu (REDIS_PASSWORD trong .env)"; return 1; fi
}

check_office() {
  if docker exec "$BACKEND" soffice --version >/dev/null 2>&1; then ok "LibreOffice có trong backend (DOCX → PDF)"
  else bad "LibreOffice không chạy được trong backend: tải file KQKN dạng DOCX sẽ không chuyển sang PDF"; fi
}

check_http() { # tên, url
  local code; code="$(curl -s -o /dev/null -m 3 -w '%{http_code}' "$2" || true)"
  if [[ "$code" =~ ^(200|301|302|401)$ ]]; then ok "$1 $2"; else bad "$1 $2 chưa chạy"; fi
}

cmd="${1:-start}"
case "$cmd" in
  start)
    command -v docker >/dev/null || die "Chưa cài Docker."
    [[ -f "$ENV_FILE" ]] || die "Thiếu file .env ở $ROOT."
    say "Kiểm tra kết nối (không bật/tắt gì):"
    failed=0
    check_mysql || failed=1
    check_redis || failed=1
    [[ $failed -eq 0 ]] || die "Thiếu MySQL hoặc Redis nên chưa chạy backend. Bật chúng lên rồi chạy lại."

    # Backend chế độ dev (run-be-docker.sh) cũng dùng cổng 8080: tắt để nhường cổng.
    docker rm -f cfc-lab-backend-office >/dev/null 2>&1 || true
    say "Build + chạy Ứng dụng, Tra cứu, API…"
    compose up -d --build
    say "Chờ API khởi động…"
    for _ in $(seq 1 90); do curl -s -o /dev/null -m 2 http://localhost:8080/api/v1/auth/me && break; sleep 2; done
    check_office
    echo
    check_http "Ứng dụng :" "http://localhost:5173"
    check_http "Tra cứu  :" "http://localhost:5173/tracuu/"
    check_http "API      :" "http://localhost:8080/api/v1/auth/me"
    echo "  Tắt: ./scripts/run-all.sh stop"
    ;;
  stop)
    say "Tắt Ứng dụng, Tra cứu, API (MySQL, Redis không bị đụng tới)…"
    compose down
    ;;
  status)
    check_mysql || true
    check_redis || true
    check_office
    check_http "Ứng dụng :" "http://localhost:5173"
    check_http "Tra cứu  :" "http://localhost:5173/tracuu/"
    check_http "API      :" "http://localhost:8080/api/v1/auth/me"
    ;;
  logs)
    compose logs -f --tail 100
    ;;
  *)
    die "Lệnh không hợp lệ: $cmd (dùng: start | stop | status | logs)"
    ;;
esac
