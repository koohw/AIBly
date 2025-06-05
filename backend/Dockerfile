FROM gradle:7.6-jdk17 AS build

WORKDIR /app

# 소스 코드 및 빌드 파일 복사
COPY src src
COPY build.gradle .
COPY settings.gradle .

# 애플리케이션 빌드
RUN gradle clean bootJar -x test --no-daemon

# 빌드된 JAR 파일 경로 찾기
RUN find /app/build/libs -name "*.jar" -type f | xargs -I {} cp {} /app/application.jar

# 실행 단계
FROM openjdk:17-jdk-slim

WORKDIR /app

# ✅ curl 설치 (Debian/Ubuntu 계열)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 빌드 단계에서 생성된 JAR 파일 복사
COPY --from=build /app/application.jar app.jar

# 컨테이너 포트 설정
EXPOSE 8001

# JAR 파일을 실행 (디버그 로그 활성화)
ENTRYPOINT ["java", "-Dlogging.level.root=INFO", "-Dlogging.level.ccrate=DEBUG", "-jar", "/app/app.jar"] 