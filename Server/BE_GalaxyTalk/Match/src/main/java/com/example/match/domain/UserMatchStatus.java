package com.example.match.domain;

import lombok.Data;

import java.util.Map;

@Data
// 매칭 상태 관리를 위한 객체
public class UserMatchStatus {
    private String userId;           // 사용자 ID
    private String concern;          // 사용자의 고민 내용
    private String mbti;            // 사용자의 MBTI
    private String preferredMbti;    // 선호하는 상대방 MBTI
    private MatchStatus status;      // 현재 매칭 상태
    private String matchId;          // 매칭 ID
    private boolean accepted;        // 매칭 수락 여부
    private long startTime;          // 매칭 시작 시간 (밀리초)
    private Map<String, Object> additionalInfo;  // 추가 정보
}

