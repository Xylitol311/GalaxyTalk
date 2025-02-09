# server.py
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import time
from model import SentenceSimilarity
import uvicorn

app = FastAPI()

# 모델 초기화 (글로벌하게 한 번만 로드)
model = SentenceSimilarity()

class SentencePair(BaseModel):
    sentence1: str
    sentence2: str

@app.post("/calculate-similarity")
async def compute_similarity(sentences: SentencePair):
    try:
        similarity = model.compute_similarity(sentences.sentence1, sentences.sentence2)
        
        return {
            "similarity_score": similarity
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run("server:app", host="127.0.0.1", port=8000, reload=True)