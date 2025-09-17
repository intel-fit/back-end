# 1. Gradle 빌드 (multi-stage)
FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

# 2. 실제 실행용
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
