#!/bin/bash

# Как запустить докер контейнер на основании докер образа?
#
# 1) собрать докер образ (как компиляция для класса)
# 2) запустить докер контейнер для образа

# Настройка
IMAGE_NAME=nbank-tests
TEST_PROFILE=${1:-api} # аргумент запуска
TIMESTAMP=$(date +"%Y%m%d_%H%M")
TEST_OUTPUT_DIR=./test-output/$TIMESTAMP

# Собираем Docker образ
echo ">>> Сборка тестов запущена"
docker build -t $IMAGE_NAME .

mkdir -p "$TEST_OUTPUT_DIR/logs"
mkdir -p "$TEST_OUTPUT_DIR/results"
mkdir -p "$TEST_OUTPUT_DIR/report"

# Запуск Docker контейнера с правильными параметрами
echo ">>> Тесты запущены"
docker run --rm \
  --network nbank-network \
  -v "$TEST_OUTPUT_DIR/logs":/app/logs \
  -v "$TEST_OUTPUT_DIR/results":/app/target/surefire-reports \
  -v "$TEST_OUTPUT_DIR/report":/app/target/site \
  -e TEST_PROFILE="$TEST_PROFILE" \
  -e APIBASEURL=http://backend:4111 \
  -e UIBASEURL=http://frontend \
  -e JAVA_TOOL_OPTIONS="\
    -Dselenide.remote=http://selenoid:4444/wd/hub \
    -Dselenide.baseUrl=http://frontend \
    -Dselenide.browser=chrome \
    -Dselenide.browserVersion=128.0 \
  " \
  $IMAGE_NAME

# Вывод итогов
echo ">>> Тесты завершены"
echo "Лог файл: $TEST_OUTPUT_DIR/logs/run.log"
echo "Результаты тестов: $TEST_OUTPUT_DIR/results"
echo "Репорт: $TEST_OUTPUT_DIR/report"