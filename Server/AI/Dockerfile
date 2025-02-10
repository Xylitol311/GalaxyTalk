FROM python:3.12

WORKDIR /app

# 필요한 Python 패키지 설치
COPY requirements.txt .
RUN pip install -r requirements.txt

# 애플리케이션 코드만 복사
COPY config.py server.py model.py run.py ./

# run.py를 통해 실행
CMD ["python", "run.py"]