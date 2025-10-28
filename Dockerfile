# Этап 1: сборка проекта
FROM gradle:9.1.0-jdk21 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle clean bootJar --no-daemon

# Этап 2: запуск
FROM eclipse-temurin:21-jre-alpine-3.22
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
