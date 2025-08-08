# GitHub Actions CI/CD Setup

## Созданные файлы

### 1. CI Pipeline (`.github/workflows/ci.yml`)
Основной пайплайн для непрерывной интеграции:

**Триггеры:**
- Push в ветку `develop`
- Pull Request в ветку `develop`

**Задачи:**
- **test**: Сборка, тестирование и создание артефактов
- **code-quality**: Статический анализ кода (SpotBugs, PMD, Checkstyle)
- **dependency-check**: Проверка безопасности зависимостей (OWASP)

### 2. Release Pipeline (`.github/workflows/release.yml`)
Пайплайн для создания релизов:

**Триггеры:**
- Создание тегов вида `v*`
- Создание релизов

**Задачи:**
- Сборка релизной версии
- Создание Docker образов (если настроены секреты)

### 3. Конфигурационные файлы
- `spotbugs-exclude.xml` - исключения для SpotBugs
- `owasp-dependency-check-suppressions.xml` - исключения для OWASP
- `README.md` - документация проекта

## Добавленные Maven плагины

В родительский `pom.xml` добавлены следующие плагины:

1. **SpotBugs** (v4.7.3.6) - поиск багов и потенциальных проблем
2. **PMD** (v3.21.0) - анализ качества кода
3. **Checkstyle** (v3.3.0) - проверка стиля кодирования
4. **OWASP Dependency Check** (v8.4.0) - проверка безопасности зависимостей

## Настройка репозитория

### Обязательные шаги:

1. **Создайте ветку develop:**
```bash
git checkout -b develop
git push -u origin develop
```

2. **Настройте защиту веток в GitHub:**
   - Перейдите в Settings → Branches
   - Добавьте rule для ветки `develop`
   - Включите "Require status checks to pass before merging"
   - Выберите все проверки CI

### Опциональные настройки:

3. **Для Docker деплоя (только для release pipeline):**
   - Добавьте секреты в Settings → Secrets and variables → Actions:
     - `DOCKER_USERNAME` - имя пользователя Docker Hub
     - `DOCKER_PASSWORD` - пароль или токен Docker Hub

4. **Создайте Dockerfile для каждого сервиса** (пример для api-gateway-service):
```dockerfile
FROM openjdk:17-jre-slim
VOLUME /tmp
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

## Использование

### Локальная разработка:
```bash
# Сборка
./mvnw clean compile

# Тесты
./mvnw test

# Статический анализ
./mvnw spotbugs:check pmd:check checkstyle:check

# Проверка безопасности
./mvnw org.owasp:dependency-check-maven:check
```

### Создание релиза:
```bash
# Создайте тег
git tag v1.0.0
git push origin v1.0.0

# Или создайте релиз через GitHub UI
```

## Артефакты

После выполнения пайплайна доступны следующие артефакты:
- JAR файлы приложений
- Отчеты тестирования (Surefire/Failsafe)
- Отчеты SpotBugs (XML/HTML)
- Отчеты PMD (XML/HTML)
- Отчеты Checkstyle (XML/HTML)
- Отчеты OWASP Dependency Check (HTML/XML)

## Кастомизация

### Настройка правил анализа:
- Редактируйте `spotbugs-exclude.xml` для исключений SpotBugs
- Редактируйте `owasp-dependency-check-suppressions.xml` для исключений OWASP
- Для PMD и Checkstyle правила настраиваются в `pom.xml`

### Изменение триггеров:
Редактируйте секцию `on:` в файлах workflow для изменения триггеров.

### Добавление уведомлений:
Добавьте step с уведомлениями в Slack/Teams/Email в конце пайплайна.
