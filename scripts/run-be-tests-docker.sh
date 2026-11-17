#!/bin/bash
# Chạy `mvn test` của backend trong Docker với MySQL 8.4 + Redis TẠM, tách hẳn khỏi database làm việc.
# Không đụng cổng 3306 của máy. Tự tạo và tự xóa container/network. Cần Docker và image maven:3.9.11-eclipse-temurin-21.
# Dùng: scripts/run-be-tests-docker.sh [tham số Maven, ví dụ -Dtest=SampleCodingServiceTest]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
NET=cfc-test-net; DB=cfc-test-mysql; RD=cfc-test-redis
cleanup() { docker rm -f "$DB" "$RD" >/dev/null 2>&1 || true; docker network rm "$NET" >/dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup
docker network create "$NET" >/dev/null
docker run -d --name "$DB" --network "$NET" -e MYSQL_ROOT_PASSWORD=testroot -e MYSQL_DATABASE=cfc_lab_test \
  -e MYSQL_USER=cfc_test -e MYSQL_PASSWORD=testpw mysql:8.4 >/dev/null
docker run -d --name "$RD" --network "$NET" redis:7-alpine >/dev/null
for _ in $(seq 1 60); do docker exec "$DB" mysqladmin ping -uroot -ptestroot --silent 2>/dev/null && break; sleep 3; done
docker run --rm --network "$NET" -v "$ROOT/source/be":/app -v "$HOME/.m2":/root/.m2 -w /app \
  -e KCS_ISOLATED_DB=true \
  -e DB_URL='jdbc:mysql://'"$DB"':3306/cfc_lab_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true' \
  -e DB_USERNAME=cfc_test -e DB_PASSWORD=testpw -e REDIS_HOST="$RD" \
  maven:3.9.11-eclipse-temurin-21 mvn -B test "$@"
