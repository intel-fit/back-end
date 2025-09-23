# Build stage
FROM gradle:8.10.2-jdk21 AS builder
WORKDIR /app

# Gradle 파일 복사 (와일드카드 사용으로 파일이 없어도 에러 없음)
COPY build.gradle* settings.gradle* ./
COPY gradle gradle

# 의존성 먼저 다운로드 (캐싱 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY src src
RUN gradle clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 시간대 설정
ENV TZ=Asia/Seoul

EXPOSE 8080

# 프로파일 설정 (환경변수로 제어 가능)
ENTRYPOINT ["sh", "-c", "java -jar -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker} app.jar"]