Gateway
================================================

### Основная информация
Gateway является точкой входа для всех запросов от фронтенда. Зона ответственности Gateway:
- Валидация токена
- Роутинг запросов к микросервисам

Gateway не отвечает за проверку прав пользователей, делающих запросы к микросервисам. За это отвечают сами 
микросервисы. <br>
Gateway не пропускает запросы к внутренним эндпоинтам, по которым микросервисы могут общаться друг с другом напрямую по 
REST, отвечая `403 Forbidden`.

### Используемый стек
- Spring Cloud Gateway
- Spring Security
- JJWT для валидации JWT

### Сборка и запуск Docker-образа
Перейдите в корень проекта, на один уровень с `Dockerfile`, и используйте следующие команды:
1. Сборка образа
    ```bash
    docker build -t gateway:local-stack .
    ```
2. Запуск контейнера с активным профилем `local-stack`
    ```bash
    docker run -e SPRING_PROFILES_ACTIVE=local-stack -p 8080:8080 gateway:local-stack
    ```

### Ссылки на репозиторий документации
- [Бизнес аналитика аутентификации и авторизации](https://github.com/it-mentor-community-platform/meta/blob/main/business-analytics/functionality/authentication-and-authorization.md)
- [Системная аналитика сервиса Gateway](https://github.com/it-mentor-community-platform/meta/blob/main/system-analytics/services/gateway/index.md)