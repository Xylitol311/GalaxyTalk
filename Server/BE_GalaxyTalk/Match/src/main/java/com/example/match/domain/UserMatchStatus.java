package com.example.match.domain;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
// 유저의 매칭 상태 관리를 위한 객체
public class UserMatchStatus {
    private String userId;                      // 사용자 ID
    private String concern;                     // 사용자의 고민 내용
    private int energy;
    private String mbti;                        // 사용자의 MBTI
    private String preferredMbti;               // 선호하는 상대방 MBTI
    private MatchStatus status;                 // 현재 매칭 상태 (WAITING, IN_PROGRESS, MATCHED, COMPLETED)
    private String matchId;                     // 매칭 ID (매칭이 성사된 경우 설정)
    private boolean accepted;                   // 매칭 수락 여부
    private long startTime;                     // 매칭 시작 시간 (밀리초)
    private Map<String, Object> additionalInfo; // 추가 정보
    private List<String> matchingQueues;        // 유저가 들어가있는 매칭 큐 목록 (Lazy Deletion 및 중복 관리에 활용)
}

