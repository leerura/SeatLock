# 1. Build Stage (Gradle 빌드)
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Gradle Wrapper 및 설정 파일 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 다운로드 (캐싱 레이어)
RUN ./gradlew dependencies --no-daemon || return 0

# 소스 코드 복사
COPY src ./src

# 빌드 실행 (테스트 스킵)
RUN ./gradlew clean bootJar --no-daemon -x test

# 2. Runtime Stage (실행 환경)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 빌드된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 환경변수로 서버 이름 전달 (로그 구분용)
ENV SERVER_NAME="app"

# 애플리케이션 실행
ENTRYPOINT ["java", \
    "-jar", \
    "-Dspring.profiles.active=docker", \
    "-Dserver.name=${SERVER_NAME}", \
    "app.jar"]
