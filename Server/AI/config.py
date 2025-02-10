# config.py
import os

class Config:
    # 공통 설정
    MODEL_NAME = "snunlp/KR-ELECTRA-discriminator"

class LocalConfig(Config):
    # 로컬 환경 설정
    MODEL_PATH = "./sentence_similarity_model_klue"
    HOST = "127.0.0.1"
    PORT = 8085

class ProdConfig(Config):
    # 배포 환경 설정
    MODEL_PATH = "/home/ubuntu/sentence_similarity_model_klue"
    HOST = "0.0.0.0"
    PORT = 8080

# 환경 변수에 따라 설정 선택
def get_config():
    env = os.getenv("PYTHON_ENV", "local")  # 환경변수가 없으면 'local'이 기본값
    if env == "prod":
        return ProdConfig
    return LocalConfig