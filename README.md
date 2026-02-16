# 🌌 은하수다 (GalaxyTalk)

**외로운 별들을 위한 빛나는 만남, 익명의 마음들이 전하는 따스한 위로**

![logo.png](doc/logo.png)

## 📖 목차
- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [매칭 알고리즘](#-매칭-알고리즘)
- [개발 환경 설정](#-개발-환경-설정)
- [배포 및 인프라](#-배포-및-인프라)
- [팀원 및 역할](#-팀원-및-역할)

## 🚀 프로젝트 소개

**은하수다**는 3D 우주 환경에서 진행되는 익명 힐링 채팅 서비스입니다. AI 기반 고민 유사도 매칭과 MBTI 분석을 통해 정서적 교감이 가능한 상대를 연결해주며, 실시간 화상/음성 채팅과 함께 마음의 위로를 찾을 수 있는 안전한 공간을 제공합니다.

### ✨ 핵심 가치
- **익명성 보장**: 행성 기반 아바타 시스템으로 안전한 소통 환경
- **AI 기반 매칭**: 고민 내용 유사도와 MBTI 분석을 통한 정확한 매칭
- **3D 몰입감**: React Three Fiber 기반 우주 테마 인터페이스
- **다양한 소통 방식**: 텍스트, 음성, 화상 채팅 + AI 대화 도우미
- **힐링의 기록**: 채팅 후 편지를 통해 따뜻한 위로의 마음 전달

## 🛠️ 주요 기능

### 👤 사용자 경험
- **행성 아바타 시스템**: 4개 행성(솔라리스, 루나리아, 테라피스, 판타지아) 중 선택
- **소셜 로그인**: 카카오 OAuth를 통한 간편 인증
- **에너지 시스템**: 사용자 활동 정도 및 신뢰도 표시
- **MBTI 기반 매칭**: 선호 성격 유형 설정으로 호환성 향상

### 🎮 3D 인터랙션
- **몰입형 우주 환경**: React Three Fiber 기반 실시간 3D 렌더링
- **안테나 매칭**: 3D 안테나 클릭으로 매칭 시작
- **눈꽃집 프로필**: 3D 집 모델로 개인 정보 접근
- **실시간 사용자 시각화**: 매칭 대기 중인 사용자들을 떠다니는 별로 표시

### 💬 채팅 및 소통
- **화상/음성 채팅**: LiveKit 기반 고품질 실시간 통신
- **텍스트 채팅**: 실시간 메시지 동기화
- **AI 대화 도우미**: OpenAI 기반 대화 주제 제안
- **리액션 시스템**: 실시간 이모지 반응
- **편지 교환**: 채팅 종료 후 후기 주고받기

### 🧠 지능형 매칭
- **AI 유사도 분석**: KR-ELECTRA 모델 기반 한국어 문장 유사도 측정
- **멀티 레벨 매칭**: Strict(완벽 MBTI 매칭) + Relaxed(부분 호환) 알고리즘
- **실시간 대기열**: WebSocket 기반 매칭 상태 실시간 업데이트
- **거절 히스토리**: 이전 거절 상대 재매칭 방지

## 🛠️ 기술 스택

### 🎨 Frontend
**React 19 + TypeScript + Vite**
- **3D Graphics**: React Three Fiber, Drei, Postprocessing
- **실시간 통신**: LiveKit (WebRTC), STOMP, SockJS
- **상태 관리**: Zustand, TanStack Query
- **UI/UX**: shadcn/ui, Tailwind CSS, Motion
- **폼 처리**: React Hook Form + Zod
- **모바일**: Capacitor (Android/iOS)
- **개발 도구**: ESLint, Prettier, MSW

### ⚙️ Backend
**Spring Boot 3.4 + Java 17 + Gradle**
- **마이크로서비스**: Auth, Chat, Match, Gateway, Eureka, Support
- **API Gateway**: Spring Cloud Gateway + JWT 인증
- **서비스 디스커버리**: Eureka Server
- **보안**: Spring Security + OAuth2 + JWT
- **실시간**: WebSocket (STOMP), LiveKit
- **AI 서비스**: FastAPI + PyTorch (KR-ELECTRA)
- **외부 API**: OpenAI GPT, 카카오/네이버 OAuth

### 🗄️ Database
- **MySQL**: 사용자, 행성, 편지, 피드백
- **MongoDB**: 채팅방, 메시지 히스토리
- **Redis**: 매칭 상태, 세션, 캐시

### 🚀 Infrastructure
**Docker + Docker Compose + Nginx**
- **CI/CD**: Jenkins Pipeline
- **보안**: SSL/TLS (Let's Encrypt)
- **모니터링**: Health Check, Logging
- **배포**: Ubuntu Server + 리버스 프록시

## 🏗️ 시스템 아키텍처

![system architecture](doc/system%20architecture.png)

### 🔐 보안 및 인증
- **API Gateway 중앙 집중식 인증**: 모든 서비스 요청의 JWT 토큰 검증
- **Refresh Token Rotation**: 보안 강화를 위한 토큰 갱신 전략 (액세스 1분, 리프레시 3일)
- **OAuth2 소셜 로그인**: 카카오/네이버 통합 인증
- **HTTPS 강제**: Let's Encrypt SSL 인증서 적용

### 📡 실시간 통신 아키텍처
- **WebSocket (STOMP + SockJS)**: 매칭 알림, 사용자 상태 브로드캐스트
- **LiveKit WebRTC**: P2P 고품질 영상/음성 통신
- **Redis Pub/Sub**: 마이크로서비스 간 실시간 이벤트 전파

### 🤖 AI 기반 서비스 통합
- **한국어 문장 유사도 분석**: KR-ELECTRA 모델 기반 고민 내용 매칭 (80% 가중치)
- **MBTI 호환성 분석**: 성격 유형 기반 매칭 알고리즘 (20% 가중치)
- **OpenAI 대화 도우미**: 상황별 대화 주제 제안 및 아이스브레이킹

### 🚀 배포 및 인프라
- **컨테이너 오케스트레이션**: Docker Compose로 7개 마이크로서비스 통합 관리
- **CI/CD 파이프라인**: Jenkins 기반 자동 빌드/배포
- **로드 밸런싱**: Nginx 리버스 프록시를 통한 트래픽 분산
- **서비스 디스커버리**: Eureka 기반 동적 서비스 등록/해제
- **헬스 체크**: 각 서비스별 상태 모니터링 및 자동 복구

## 🧩 매칭 알고리즘

### 🎯 지능형 매칭 플로우
1. **사용자 입력** - 두 사용자가 각각 고민 내용 입력
2. **AI 유사도 분석** - 입력된 고민 내용의 유사도 계산 (가중치 80%)
3. **MBTI 호환성 분석** - 사용자 성격 유형 간 호환성 분석 (가중치 20%)
4. **종합 점수 계산** - AI 유사도 × 0.8 + MBTI 호환성 × 0.2
5. **매칭 결과** - 임계값 이상 시 매칭 성공

### 📊 매칭 점수 계산
- **고민 유사도 (80%)**: KR-ELECTRA 기반 한국어 문장 임베딩 코사인 유사도
- **MBTI 호환성 (20%)**: 
  - **Strict Mode**: 선호 MBTI 완벽 일치 (가중치 +100%)
  - **Relaxed Mode**: 글자별 부분 매칭 (4글자 중 일치 개수 비례)

### 📡 실시간 매칭 프로세스

![sequence diagram](doc/sequence%20diagram.png)

1. **매칭 요청**
   ```
   사용자 고민 입력 (10-100자) → 선호 MBTI 선택 → WAITING 상태 전환 → 대기 큐 입장
   ```

2. **스케줄링 매칭 (5초 주기)**
   ```
   WAITING 사용자 수집 → AI 유사도 계산 → MBTI 호환성 분석 → 최적 쌍 선정 → 원자적 상태 전환
   ```

3. **매칭 알림 및 응답**
   ```
   IN_PROGRESS → MATCHED → 3초 지연 알림 → 1분 응답 대기 → 수락/거절 처리
   ```

4. **채팅방 생성**
   ```
   양측 수락 → LiveKit 토큰 생성 → 채팅방 ID 발급 → WebSocket 전송 → CHATTING 상태
   ```

### 🛡️ 매칭 품질 보장
- **거절 히스토리 관리**: 이전 거절 상대 재매칭 방지
- **Lua 스크립트 원자성**: Redis 기반 동시성 제어
- **자동 타임아웃**: 1분 무응답 시 자동 매칭 취소
- **실시간 알림**: WebSocket 기반 즉시 상태 업데이트

## 💻 개발 환경 설정

### 🎨 Frontend 개발
```bash
# 프로젝트 루트에서
cd Client
npm install
npm run dev  # 개발 서버 시작 (localhost:5173)
```

### ⚙️ Backend 개발
```bash
# 각 서비스별 개발 서버 시작
cd Server/BE_GalaxyTalk/[Service Name]
./gradlew bootRun

# 또는 Docker Compose로 전체 환경 구성
cd Server
docker-compose up -d
```

### 🗃️ 데이터베이스 설정
- **MySQL**: 사용자 정보, 행성, 편지, 피드백 스키마
- **MongoDB**: 채팅방 및 메시지 컬렉션
- **Redis**: 매칭 상태 및 세션 관리

## 🚀 배포 및 인프라

### 🐳 Docker 기반 배포
```yaml
# docker-compose.yml 구조
services:
  - nginx (리버스 프록시)
  - gateway (API 게이트웨이)
  - eureka (서비스 디스커버리)
  - auth, chat, match, support (마이크로서비스)
  - ai-service (AI 매칭)
  - mysql, mongodb, redis (데이터베이스)
  - livekit (WebRTC 서버)
```

### 🔧 CI/CD 파이프라인
- **Jenkins**: 자동 빌드 및 배포
- **GitHub Webhooks**: 코드 푸시 시 자동 트리거
- **Docker Registry**: 컨테이너 이미지 관리
- **SSL 인증서**: Let's Encrypt 자동 갱신

### 📊 모니터링 및 로깅
- **헬스 체크**: 각 서비스별 상태 모니터링
- **Nginx 로그**: 접근 로그 및 에러 추적
- **Docker 로그**: 컨테이너별 로그 집계

## 👥 팀원 및 역할

| 이름 | 역할 | 주요 담당 영역 |
| --- | --- | --- |
| 민인애 | **Frontend Developer** | React 3D UI, 사용자 경험 설계, 컴포넌트 아키텍처 |
| 박유진 | **Frontend Developer** | 실시간 통신, 상태 관리, 모바일 최적화 |
| 김준형 | **DevOps Engineer** | Docker 배포, CI/CD 파이프라인, 인프라 관리 |
| 박도아 | **Backend Developer** | 인증 서비스, API Gateway, 보안 구현 |
| 차수홍 | **Team Leader & Backend Developer** | 매칭 서비스, 실시간 통신, 성능 최적화 |
| 홍찬우 | **Backend & AI Developer** | 채팅 서비스, 데이터베이스 설계, AI 서비스 |