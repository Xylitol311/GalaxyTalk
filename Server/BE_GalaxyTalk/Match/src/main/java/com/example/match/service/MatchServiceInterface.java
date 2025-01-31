package com.example.match.service;

import com.example.match.domain.MatchResponse;
import com.example.match.domain.UserMatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * 매칭 서비스 인터페이스
 * 사용자 매칭과 관련된 핵심 기능들을 정의합니다.
 */
public interface MatchServiceInterface {
    /**
     * 새로운 매칭 요청을 처리합니다.
     * 이 메서드는 다음과 같은 작업을 수행합니다:
     * 1. 사용자 상태를 Redis에 저장
     * 2. 대기열에 사용자 추가
     * 3. 매칭 프로세스 시작
     * 4. WebSocket을 통한 대기 상태 알림
     *
     * @param user 매칭을 요청한 사용자의 상태 정보
     * @throws MatchingException 매칭 처리 중 오류 발생 시
     */
    void startMatching(UserMatchStatus user);

    /**
     * 매칭 응답(수락/거절)을 처리합니다.
     * 이 메서드는 다음과 같은 작업을 수행합니다:
     * 1. 매칭 수락 시: 상대방의 수락 여부 확인 후 채팅방 생성
     * 2. 매칭 거절 시: 양측 상태 초기화 및 재매칭 프로세스 시작
     *
     * @param response 매칭에 대한 사용자의 응답 정보
     * @throws MatchingException 응답 처리 중 오류 발생 시
     */
    void processMatchResponse(MatchResponse response);

    /**
     * 타임아웃된 사용자의 선택을 처리합니다.
     * 사용자가 선택할 수 있는 옵션:
     * 1. 추가 대기
     * 2. 매칭 조건 완화
     * 3. 다음 세션으로 이동
     *
     * @param userId 사용자 ID
     * @param choice 사용자가 선택한 옵션
     */
    void handleTimeoutChoice(String userId, String choice);

    /**
     * 매칭 서비스의 현재 상태 정보를 조회합니다.
     * 반환되는 정보:
     * - 현재 대기 중인 사용자 수
     * - 최근 매칭 성공률
     * - 평균 대기 시간
     *
     * @return 매칭 서비스의 현재 상태 정보
     */
//    MatchServiceStatus getServiceStatus();
}

/**
 * 매칭 서비스의 현재 상태를 나타내는 클래스
 */
@Data
@AllArgsConstructor
class MatchServiceStatus {
    private int waitingUsersCount;     // 현재 대기 중인 사용자 수
    private double matchSuccessRate;    // 최근 매칭 성공률 (%)
    private long averageWaitingTime;    // 평균 대기 시간 (밀리초)
    private Map<String, Integer> mbtiDistribution;  // MBTI별 대기자 수 분포
}

/**
 * 매칭 관련 예외를 처리하는 클래스
 */
class MatchingException extends RuntimeException {
    public MatchingException(String message) {
        super(message);
    }

    public MatchingException(String message, Throwable cause) {
        super(message, cause);
    }
}
