# Build stage
FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
RUN gradle dependencies --no-daemon || true
COPY src src
RUN gradle clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health check
RUN apk add --no-cache curl

# Copy jar file
COPY --from=builder /app/build/libs/*.jar app.jar

# Set timezone
ENV TZ=Asia/Seoul

EXPOSE 8080

# Use environment variable for profile, default to docker
ENTRYPOINT ["sh", "-c", "java -jar -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker} app.jar"]