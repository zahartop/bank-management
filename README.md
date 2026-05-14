# Bank Management

REST API для управления банковскими картами: JWT, роли **ADMIN** / **USER**, шифрование PAN (AES‑256‑GCM), переводы между своими картами с пессимистическими блокировками, Liquibase, Docker, CI с проверкой секретов и уязвимостей.

> **Сдать тестовое:** публичный репозиторий на GitHub (архив ZIP по условиям задания не подходит). В `README` ниже — как запустить и что проверить перед отправкой.

---

## Статус CI

Замени `YOUR_USER` и при необходимости имя репозитория в URL бейджа.

```markdown
![CI](https://github.com/YOUR_USER/bank-management/actions/workflows/ci.yml/badge.svg)
```

В pipeline (три job’а, легко расширять):

| Job | Зачем |
|-----|--------|
| **`secrets-scan`** | Gitleaks + Trivy fs (секреты). Без JDK, полный `fetch-depth` только здесь. |
| **`maven-verify`** | Вызывает [reusable-maven-verify.yml](.github/workflows/reusable-maven-verify.yml): `mvn verify`. Матрица в [`ci.yml`](.github/workflows/ci.yml) через `matrix.include` — поля `java-version` и **`maven-args`** (для мульти-модуля, например `-pl billing -am`). Добавь вторую строку `include` с `java-version: "21"`, когда проект перейдёт на JDK 21. |
| **`container-scan`** | Сборка Docker-образа + Trivy image (HIGH/CRITICAL в лог; `exit-code: 0` у образа — при желании поменяй на `1`). |

**Масштабирование / монорепо:** в [`ci.yml`](.github/workflows/ci.yml) в `matrix.include` задай **`maven-args`** (аргументы Maven для подмодулей). В `with:` у `workflow_call` нельзя использовать `env` — поэтому аргументы идут через матрицу, а не через `env.MAVEN_ARGS`. Родительский репозиторий может вызывать `reusable-maven-verify.yml` и передать `maven-args` и **`working-directory`**.

**Checkstyle:** подавления подключаются через **`suppressionsLocation`** в `pom.xml` (Maven сам мержит [`suppressions.xml`](config/checkstyle/suppressions.xml) с основным конфигом — без `${config_loc}`).

Локально перед пушем:

```bash
mvn -B verify
```

---

## Стек

| Технология | Версия / заметка |
|------------|------------------|
| Java | 17 |
| Spring Boot | 3.3.x |
| Spring Security | JWT (stateless) |
| PostgreSQL | 16 (в compose) |
| Liquibase | `src/main/resources/db/migration/` |
| MapStruct, Lombok | да |
| OpenAPI / Swagger UI | `/swagger-ui.html`, `/v3/api-docs` |
| Actuator | **`/health`**, **`/info`** (без префикса `/actuator`) |

---

## Быстрый старт (Docker)

Из корня репозитория:

```bash
docker compose up --build
```

Поднимутся **PostgreSQL** и **app**. Профиль `docker` подставляет JDBC на хост `postgres`.

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/health`

### Переменные окружения (важно для продакшена)

В **production** задай сильные значения (не коммить в git):

| Переменная | Назначение |
|------------|------------|
| `APP_JWT_SECRET` | Секрет для подписи JWT (строка; внутри усиливается через SHA‑256 под ключ HS256). |
| `APP_CRYPTO_PAN_SECRET` | Секрет для шифрования номера карты в БД. |

В `docker-compose.yml` сейчас **только демо-значения** для локального запуска — для реальной сдачи/прода замени на свои секреты через env / secrets manager, не публикуй реальные ключи.

---

## Локальный запуск без Docker (только БД в compose)

```bash
docker compose up -d postgres
export APP_JWT_SECRET='минимум-32-символа-для-локальной-разработки!!'
export APP_CRYPTO_PAN_SECRET='минимум-16-символов-для-pan'
mvn spring-boot:run
```

База по умолчанию: `jdbc:postgresql://localhost:5432/bank_management`, пользователь `bank` / `bank` (как в `docker-compose.yml`).

---

## API в двух словах

1. **Логин:** `POST /api/v1/auth/login` — тело `{"username":"...","password":"..."}` → JWT.
2. **Карты:** `POST /api/v1/cards`, `GET /api/v1/cards/{id}`, `GET /api/v1/cards` (пагинация). В ответах PAN **только маска** (`4111 **** **** 1111`).
3. **Перевод:** `POST /api/v1/transfers` — между двумя **своими** картами, обе `ACTIVE`, проверка баланса; строки карт блокируются `FOR UPDATE` в фиксированном порядке по `id`, чтобы снизить риск deadlock.

**Пользователей в БД из коробки нет** — для проверки создай пользователя (через SQL после миграций или свой скрипт) с паролем в виде **BCrypt**‑хеша. Пример для собеседования: один `ADMIN`, один `USER`.

---

## Безопасность данных

- **PAN в PostgreSQL** хранится зашифрованным (`PanEncryptionConverter`, AES‑256‑GCM).
- В JSON наружу уходит только **маскированный** номер.
- **Не клади** реальные `APP_*` секреты и пароли в коммиты: CI с **Gitleaks** и **Trivy secret** на это рассчитан. В репозитории намеренно остаются только **шаблонные** строки в указанных yaml/compose — они исключены из fs‑скана Trivy и частично из allowlist Gitleaks для плейсхолдеров; настоящие секреты туда не пиши.

---

## Тесты

```bash
mvn verify
```

Интеграционный тест с **Testcontainers** (`CardLifecycleIntegrationTest`) требует **Docker** на машине; если Docker недоступен, класс помечен `@Testcontainers(disabledWithoutDocker = true)` и тест **пропускается** (сборка не падает). На **GitHub Actions** Docker есть — там тест выполняется.

---

## Структура проекта (кратко)

```
src/main/java/com/bank/management/
  api/controller/     # REST + OpenAPI аннотации
  api/error/          # GlobalExceptionHandler, ErrorResponse
  dto/                # Request/Response + MapStruct mapper
  entity/             # JPA сущности
  repository/         # Spring Data JPA (+ pessimistic lock для переводов)
  security/           # JWT filter, SecurityConfig
  service/            # Бизнес-логика
  persistence/crypto/ # Шифрование PAN
```

---

## Честный roadmap (если развивать дальше)

- Журнал переводов / outbox + Kafka для аудита.
- Refresh‑токены, revoke/blacklist `jti`.
- Лимиты переводов и идемпотентность (`Idempotency-Key`).

---

## Лицензия

Укажи свою лицензию или оставь «только для тестового задания», если так требует работодатель.

---

**Удачи с ревью и собесом.** Если что-то в CI красное — сначала смотри лог Gitleaks/Trivy: чаще всего это реально забытый токен в diff, а не «ложняя тревога».
