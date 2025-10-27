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


### Ссылки на репозиторий документации
- [Бизнес аналитика аутентификации и авторизации](https://github.com/it-mentor-community-platform/meta/blob/main/business-analytics/functionality/authentication-and-authorization.md)
- [Системная аналитика сервиса Gateway](https://github.com/it-mentor-community-platform/meta/blob/main/system-analytics/services/gateway/index.md)