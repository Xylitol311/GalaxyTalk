package com.example.match.domain;

// 매칭 상태를 표현하는 enum
public enum MatchStatus {
    WAITING,       // 매칭 대기 중
    IN_PROGRESS,   // 매칭 작업 중 (비동기 매칭 처리 시 잠시 상태 변경)
    MATCHED       // 매칭이 성사된 상태
}
