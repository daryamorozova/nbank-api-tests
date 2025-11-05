#!/usr/bin/env bash
set -euo pipefail

# ===============================
# Настройки
# ===============================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
DOCKER_COMPOSE_FILE="$PROJECT_ROOT/infra/docker_compose/docker-compose.yml"

IMAGE_NAME=nbank-tests
TEST_PROFILE=${1:-api}
TIMESTAMP=$(date +"%Y%m%d_%H%M")
TEST_OUTPUT_DIR="$PROJECT_ROOT/test-output/$TIMESTAMP"
WAIT_SECONDS=${WAIT_SECONDS:-20}
DOCKER_NETWORK=nbank-network

# ===============================
# Выбор команды compose (v2 или v1)
# ===============================

if docker compose version >/dev/null 2>&1; then
  COMPOSE="docker compose"
elif docker-compose version >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  echo "Ошибка: не найден Docker Compose (ни 'docker compose', ни 'docker-compose'). Установи Docker Compose." >&2
  exit 1
fi

# ===============================
# Проверки
# ===============================

if ! command -v docker >/dev/null 2>&1; then
  echo "Ошибка: docker не найден в PATH. Установи Docker и попробуй снова." >&2
  exit 1
fi

if [ ! -f "$DOCKER_COMPOSE_FILE" ]; then
  echo "Ошибка: не найден docker-compose.yml по пути: $DOCKER_COMPOSE_FILE" >&2
  exit 1
fi

# ===============================
# Очистка окружения при выходе
# ===============================

cleanup() {
  echo ">>> Останавливаем тестовое окружение ($COMPOSE down)"
  # Без лишних флагов — чтобы одинаково работало и с v1, и с v2
  $COMPOSE -f "$DOCKER_COMPOSE_FILE" down || true
}
trap cleanup EXIT

# ===============================
# Сборка образа с тестами
# ===============================

echo ">>> Сборка Docker-образа с тестами: ${IMAGE_NAME}"
docker build -t "$IMAGE_NAME" "$PROJECT_ROOT"

mkdir -p "$TEST_OUTPUT_DIR/logs" "$TEST_OUTPUT_DIR/results" "$TEST_OUTPUT_DIR/report"

# ===============================
# Поднимаем тестовое окружение
# ===============================

echo ">>> Поднимаем тестовое окружение через $COMPOSE"
echo "    Файл: $DOCKER_COMPOSE_FILE"
$COMPOSE -f "$DOCKER_COMPOSE_FILE" up -d

echo ">>> Ждём ${WAIT_SECONDS} секунд, пока сервисы поднимутся..."
sleep "$WAIT_SECONDS"

echo ">>> Диагностика: список контейнеров и их здоровье"
$COMPOSE -f "$DOCKER_COMPOSE_FILE" ps

echo ">>> Диагностика: сетевые DNS-записи и доступность портов"
docker run --rm --network "$DOCKER_NETWORK" busybox nslookup backend || true
docker run --rm --network "$DOCKER_NETWORK" busybox nslookup nginx || true
docker run --rm --network "$DOCKER_NETWORK" busybox nslookup selenoid || true

docker run --rm --network "$DOCKER_NETWORK" curlimages/curl:8.10.1 -I http://backend:4111 || true
docker run --rm --network "$DOCKER_NETWORK" curlimages/curl:8.10.1 -I http://nginx || true
docker run --rm --network "$DOCKER_NETWORK" curlimages/curl:8.10.1 -I http://selenoid:4444/status || true

# ===============================
# Запуск контейнера с тестами
# ===============================

echo ">>> Запускаем тесты в контейнере"

docker run --rm \
--network "$DOCKER_NETWORK" \
  -v "$TEST_OUTPUT_DIR/logs":/app/logs \
  -v "$TEST_OUTPUT_DIR/results":/app/target/surefire-reports \
  -v "$TEST_OUTPUT_DIR/report":/app/target/site \
  -e APIBASEURL=http://backend:4111 \
  -e UIBASEURL=http://nginx \
  -e SELENOID_URL=http://selenoid:4444/wd/hub \
  -e JAVA_TOOL_OPTIONS="--enable-preview \
    -DAPIBASEURL=http://backend:4111 \
    -DUIBASEURL=http://nginx \
    -DSELENOID_URL=http://selenoid:4444/wd/hub \
    -Dselenide.remote=http://selenoid:4444/wd/hub \
    -Dselenide.browser=chrome \
    -Dselenide.browserSize=1920x1080" \
  "$IMAGE_NAME"

echo ">>> Логи сервисов:"
$COMPOSE -f "$DOCKER_COMPOSE_FILE" logs --tail=200 backend nginx selenoid || true

echo ">>> Генерируем HTML-репорт из сохранённых XML"
docker run --rm \
  -v "$TEST_OUTPUT_DIR/results":/app/target/surefire-reports:ro \
  -v "$TEST_OUTPUT_DIR/report":/app/target/site \
  "$IMAGE_NAME" \
  bash -lc "mkdir -p target/site && rm -rf target/site/* && mvn -q -DskipTests=true surefire-report:report && echo '--- target/site:' && ls -la target/site"


echo "HTML-репорт: $TEST_OUTPUT_DIR/report/surefire-report.html"


echo ">>> Тесты завершены"
echo "Лог файл:          $TEST_OUTPUT_DIR/logs/run.log"
echo "Результаты тестов: $TEST_OUTPUT_DIR/results"
echo "HTML-репорт:       $TEST_OUTPUT_DIR/report"
echo "Docker Compose окружение будет остановлено автоматически."