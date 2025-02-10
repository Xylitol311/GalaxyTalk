# 1. 기초 공사: 베이스 이미지 지정
FROM amazoncorretto:17

# 2. 작업실 마련: 작업 디렉토리 설정
WORKDIR /app

# curl 설치: apt-get update 후 curl 설치
RUN apt-get update && apt-get install -y curl

# 3. 빌드된 JAR 파일 지정 (빌드 결과물이 있는 경로에 맞게 수정)
ARG JAR_FILE=build/libs/*SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 4. wait-for-health.sh 스크립트를 컨테이너에 복사하기
#    이 스크립트는 의존 서비스들이 준비될 때까지 대기할 것입니다.
#    스크립트 파일은 Gateway 프로젝트 내에 만들어두어야 합니다.
COPY wait-for-health.sh /usr/local/bin/wait-for-health.sh

# 5. 스크립트에 실행 권한 부여
RUN chmod +x /usr/local/bin/wait-for-health.sh

# 6. 컨테이너가 사용하는 포트 공개
EXPOSE 8080

# 7. 컨테이너 시작 시 wait-for-health.sh 스크립트를 먼저 실행하도록 ENTRYPOINT 설정
ENTRYPOINT ["/usr/local/bin/wait-for-health.sh"]
