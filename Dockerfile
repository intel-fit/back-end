FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jdk
WORKDIR /app
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
# Docker 환경에서 application-docker.yml 사용
ENTRYPOINT ["java","-jar","-Dspring.profiles.active=docker","app.jar"]