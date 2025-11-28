plugins {
	java
	id("org.springframework.boot") version "3.2.10"
	id("io.spring.dependency-management") version "1.1.4"
}

group = "rto"
version = "0.0.1-SNAPSHOT"
description = "Spring Boot project intelfit"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

// Spring Boot BOM 명시적 관리
dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.10")
	}

	dependencies {
		dependency("org.springframework.boot:spring-boot:3.2.10")
		dependency("org.springframework.boot:spring-boot-starter:3.2.10")
		dependency("org.springframework.boot:spring-boot-autoconfigure:3.2.10")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation ("org.springframework.boot:spring-boot-starter-mail")
	implementation("software.amazon.awssdk:s3:2.25.66")
	// OAuth2 Client
	implementation ("org.springframework.boot:spring-boot-starter-oauth2-client")

	// HTTP Client (카카오 API 호출용)
	implementation ("org.springframework.boot:spring-boot-starter-webflux")
	//JWT
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

	// MySQL - Spring Boot 3.2.10 권장 버전
	runtimeOnly("com.mysql:mysql-connector-j")

	// Swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	// JSON 파싱
	implementation("com.fasterxml.jackson.core:jackson-databind")
	implementation ("org.springframework.boot:spring-boot-starter-validation")

	implementation ("org.springframework.boot:spring-boot-starter-web")
	implementation("com.stripe:stripe-java:22.29.0")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
