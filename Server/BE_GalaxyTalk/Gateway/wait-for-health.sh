#!/bin/sh
set -e

# --- 함수: 지정한 서비스의 /actuator/health 엔드포인트를 체크 ---
wait_for_service() {
  host=$1
  port=$2
  url="http://${host}:${port}/actuator/health"
  echo "Waiting for ${host}:${port} to be healthy..."
  while true; do
    # curl로 health 엔드포인트에 접근해서 "status":"UP" 문자열이 포함되어 있는지 확인합니다.
    if curl -s "$url" | grep -q '"status":"UP"'; then
      echo "${host}:${port} is healthy!"
      break
    fi
    echo "Service ${host}:${port} is not healthy yet. Waiting 5 seconds..."
    sleep 5
  done
}

# --- 의존 서비스들이 준비될 때까지 대기 ---
# 아래의 서비스 이름과 포트 번호는 docker-compose.yml에서 정의한 서비스 이름과 내부 포트와 일치해야 합니다.
wait_for_service "eureka" "8761"
wait_for_service "galaxy-auth" "8080"
wait_for_service "galaxy-match" "8080"
wait_for_service "galaxy-support" "8080"

echo "All dependent services are healthy. Starting Gateway..."

# 모든 서비스가 준비되었으면 실제 애플리케이션 실행
exec java -jar /app/app.jar
