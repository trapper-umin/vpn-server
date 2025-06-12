# API Gateway Service

API Gateway сервис является единой точкой входа в систему VPN. Он обеспечивает маршрутизацию запросов к микросервисам и контроль доступа на основе JWT токенов.

## Возможности

- **Маршрутизация запросов** к микросервисам через Consul Service Discovery
- **JWT аутентификация** для защищенных маршрутов
- **Глобальная фильтрация** запросов
- **Load balancing** между экземплярами микросервисов
- **Мониторинг** через Spring Boot Actuator

## Технологии

- Spring Cloud Gateway
- Spring Cloud Consul Discovery
- JWT (jjwt)
- Lombok

## Маршруты

### Публичные маршруты (без авторизации):
- `/api/auth/**` - Аутентификация и регистрация
- `/api/payments/notify` - Webhook уведомления от платежных систем
- `/actuator/health` - Health check

### Защищенные маршруты (требуют JWT токен):
- `/api/user/**` → user-service
- `/api/subscription/**` → subscription-service
- `/api/vpn/**` → vpn-service
- `/api/payments/**` → payment-service (кроме `/notify`)

## Конфигурация

### JWT
```yaml
jwt:
  secret: mySecretKey123456789012345678901234567890
  expiration: 86400000 # 24 часа
```

### Consul
```yaml
spring:
  cloud:
    consul:
      host: localhost
      port: 8500
      discovery:
        enabled: true
        register: true
```

## Структура проекта

```
src/main/java/server/vpn/com/gateway/
├── config/              # Конфигурационные классы
│   ├── JwtProperties.java
│   ├── SecurityProperties.java
│   └── GatewayConfig.java
├── security/           # Классы безопасности
│   ├── JwtUtil.java
│   └── JwtAuthenticationFilter.java
└── ApiGatewayServiceApplication.java
```

## Запуск

1. Убедитесь, что Consul запущен на `localhost:8500`
2. Запустите приложение:
```bash
./mvnw spring-boot:run
```
3. API Gateway будет доступен на порту 8080

## Мониторинг

- Health check: `GET http://localhost:8080/actuator/health`
- Gateway routes: `GET http://localhost:8080/actuator/gateway/routes`
- Gateway info: `GET http://localhost:8080/actuator/info`

## Использование JWT

Для доступа к защищенным маршрутам добавьте заголовок:
```
Authorization: Bearer <your-jwt-token>
```

Фильтр автоматически:
1. Извлекает токен из заголовка Authorization
2. Валидирует токен
3. Добавляет заголовок `X-User-Id` с именем пользователя для микросервисов
4. Пропускает запрос или возвращает 401 Unauthorized 