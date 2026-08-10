# Voice Planner

Telegram-first ассистент, который превращает голосовые заметки в обязательства и возвращает их в нужный момент.

## Стек

- Java 25 LTS
- Spring Boot 4.1
- PostgreSQL 18
- Flyway
- Gradle 9.5
- Docker Compose
- Testcontainers
- GitHub Actions и Dependabot

## Локальный запуск

Требования: JDK 25 и Docker.

1. Запустить PostgreSQL:

   ```powershell
   docker compose up -d postgres
   ```

   PostgreSQL проекта доступен с хоста на `localhost:5433`; порт `5432` оставлен свободным для локально установленного PostgreSQL.

2. Запустить приложение:

   ```powershell
   .\scripts\gradle-win.ps1 bootRun
   ```

3. Проверить готовность:

   ```text
   http://localhost:8080/actuator/health/readiness
   ```

Чтобы собрать и запустить приложение целиком в контейнерах:

```powershell
docker compose --profile app up --build
```

Локальные значения можно изменить через `.env`; пример находится в `.env.example`. Настоящие секреты в Git не добавляются.

## Проверки

```powershell
.\scripts\gradle-win.ps1 build
docker compose config
docker build -t voice-planner:local .
```

Интеграционный тест поднимает временный PostgreSQL через Testcontainers, поэтому Docker должен быть доступен.

Скрипт `scripts/gradle-win.ps1` также обходит проблему Gradle/JVM с кириллицей в Windows-пути: при необходимости он временно отображает папку проекта на свободную букву диска и удаляет это отображение после команды. На Linux и macOS используется обычный `./gradlew`.

## Конфигурация production

Приложение ожидает переменные:

- `DATABASE_URL` — JDBC URL PostgreSQL;
- `DATABASE_USERNAME`;
- `DATABASE_PASSWORD`;
- `DATABASE_POOL_SIZE`;
- `PORT`;
- `APP_LOG_LEVEL`.

В production пароль базы должен поступать из хранилища секретов выбранной платформы. Файл `.env` предназначен только для локальной разработки.

## CI

Workflow `.github/workflows/ci.yml` запускается для push и pull request:

- проверяет Gradle Wrapper;
- собирает приложение и запускает тесты;
- сохраняет отчёты упавших тестов;
- отдельно проверяет сборку production Docker-образа.

Dependabot еженедельно проверяет Gradle, GitHub Actions и Docker-образы.

CD не добавлен намеренно: сначала нужно выбрать площадку размещения и определить способ управления production-секретами.

## Продуктовые документы

- [Продуктовая спецификация MVP](docs/MVP_PRODUCT_SPEC.md)
- [План проверки голосовых сценариев](docs/VOICE_VALIDATION.md)
