# 1. 기초 공사: 베이스 이미지 지정
FROM amazoncorretto:17

# 2. 작업실 마련: 작업 디렉토리 설정
WORKDIR /app

ARG JAR_FILE=build/libs/*SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


