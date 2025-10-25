# Build stage - Spring Boot 3.2.10 호환 버전으로 변경
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# 환경변수 설정 (Spring Boot 버전 고정)
ENV SPRING_BOOT_VERSION=3.2.10

# Gradle 관련 파일들 복사 (Kotlin DSL 지원)
COPY gradle gradle
COPY gradlew gradlew.bat build.gradle* settings.gradle* ./

# 권한 설정
COPY src src

RUN chmod +x ./gradlew

# 의존성 다운로드 (캐싱 최적화)
RUN ./gradlew dependencies --no-daemon --refresh-dependencies

# 소스 코드 복사 및 빌드
RUN ./gradlew clean bootJar --no-daemon --refresh-dependencies

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 필요한 패키지 설치
RUN apk add --no-cache curl tzdata

# 시간대 설정
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 앱 실행용 사용자 생성 (보안)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# JAR 파일 복사
COPY --from=builder --chown=appuser:appgroup /app/build/libs/*.jar app.jar

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM 튜닝 옵션 추가
ENTRYPOINT ["sh", "-c", "java -Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker} -jar app.jar"]