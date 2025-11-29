### STAGE 1 : BUILD
FROM eclipse-temurin:21-jdk-alpine AS build

# 작업 디렉토리 설정
WORKDIR /leum-server

# 의존성의 빠른 변화감지를 위함
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 설치 진행
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사
COPY src ./src

# 최종 빌드 - Jar 파일 생성 (테스트는 생략)
RUN ./gradlew bootJar --no-daemon -x test

### STAGE 2 : PRODUCTION
FROM eclipse-temurin:21-jre-alpine AS production

# 사용자 및 그룹설정
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
WORKDIR /leum-server

# 빌드 결과물 복사
COPY --from=build /leum-server/build/libs/leum-server.jar leum-server.jar

# 포트 설정 (8080 - 앱, 8081 - 헬스체크)
EXPOSE 8080
EXPOSE 8081

# 최종 실행 명령어
CMD [ "java", "-jar", "leum-server.jar" ]