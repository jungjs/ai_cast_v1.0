FROM eclipse-temurin:17-jre-alpine

# 시스템 타임존 설정
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apk del tzdata

# 애플리케이션 사용자 및 그룹 생성
RUN addgroup -S aicast && adduser -S aicast -G aicast

# 작업 디렉토리 생성 및 소유권 변경
WORKDIR /app
RUN mkdir -p /app/logs && chown -R aicast:aicast /app

# JAR 파일 복사
COPY target/ai_cast-1.0.0-SNAPSHOT.jar app.jar

# 폰트 등 정적 리소스 복사 (필요 시)
# COPY src/main/resources/fonts /app/fonts

# 사용자 전환
USER aicast

EXPOSE 8080

# JVM 최적화 옵션 (컨테이너 환경) 및 실행
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
