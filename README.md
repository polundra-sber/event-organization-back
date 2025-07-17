# Event Organization Backend

**Многофункциональное веб-приложение для организации частных мероприятий**  
Бэкенд платформы для совместного планирования событий, управления списками задач/покупок и распределения расходов между участниками.

[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-stanleystanmarsh%2Fevent--organization--app%3A1.0.2-blue?logo=docker)](https://hub.docker.com/r/stanleystanmarsh/event-organization-app)

## Описание

Данный проект реализует серверную часть системы для организации и управления частными мероприятиями (дни рождения, корпоративы, встречи и др.). Сервис позволяет создавать события, приглашать участников, вести списки задач и покупок, а также автоматически распределять расходы между всеми участниками.

## Основные возможности
- Регистрация и аутентификация пользователей (JWT)
- Создание и управление событиями
- Приглашение и управление участниками
- Ведение списков задач и покупок
- Автоматическое распределение расходов и учёт долгов
- Просмотр личных долгов, доходов и покупок
- Гибкая система ролей участников события
- REST API с документацией OpenAPI 3.0

## Технологический стек
- **Язык**: Java 23
- **Фреймворк**: Spring Boot 3.5.0 (Reactive)
- **База данных**: PostgreSQL + R2DBC
- **Аутентификация**: JWT
- **Документация API**: OpenAPI 3.0
- **Билд-система**: Maven
- **Контейнеризация**: [деплой через Docker](https://github.com/polundra-sber/event-organization-deployment)

## Структура проекта
- `src/main/java/ru/eventorg/` — основной исходный код приложения
  - `controller/` — REST-контроллеры
  - `service/` — бизнес-логика
  - `repository/` — доступ к данным (R2DBC)
  - `entity/` — сущности базы данных
  - `dto/` — объекты передачи данных
  - `exception/` — обработка ошибок
  - `security/` — безопасность и JWT
  - `config/` — конфигурация приложения
- `src/main/resources/` — ресурсы приложения (application.yaml, [openapi.yaml](#документация-api))
- `initdb/` — скрипты инициализации БД
- `Dockerfile` — контейнер Docker

## Быстрый старт

### 1. Клонирование репозитория
```bash
git clone https://github.com/polundra-sber/event-organization-back.git
cd event-organization-back
```

### 2. Локальный запуск через Docker
Можно создать свой `docker-compose.yml` или воспользоваться [готовым](https://github.com/polundra-sber/event-organization-deployment).
```bash
docker-compose up --build
```

### 3. Ручной запуск (без Docker)
- Убедитесь, что PostgreSQL запущен и параметры подключения указаны в `src/main/resources/application.yaml`
- Соберите проект:
```bash
mvn clean package
```
- Запустите приложение:
```bash
java -jar target/event-organization-*.jar
```

## Документация API
- [OpenAPI Documentation](https://petstore.swagger.io/?url=https://raw.githubusercontent.com/polundra-sber/event-organization-back/refs/heads/master/src/main/resources/openapi.yaml)
- Файл спецификации: `src/main/resources/openapi.yaml`

## Контакты
- © 2025 [Астафьев Игорь](https://github.com/StanleyStanMarsh), [Богданова Елизавета](https://github.com/lizochka-bogdanovaa), [Ложкина Анастасия](https://github.com/alozhkina), [Луговенко Полина](https://github.com/polyalugovenko), [Якунин Дмитрий](https://github.com/tutibase). Все права защищены.
- Issues и предложения: через [GitHub Issues](https://github.com/polundra-sber/event-organization-back/issues)