package com.example.match.domain;

import lombok.Data;

@Data
// 매칭 상태 관리를 위한 객체
public class UserMatchStatus {
    private String userId; // userId: 네이버 api에서 받은 일련번호
    private String concern; // 유저가 입력한 고민
    private String preferredMbti; // 유저가 선호하는 MBTI
    private int age; // 유저의 연령대
    private MatchStatus status; // 매칭 상태(대기, 매칭, 완료)
    private String matchId; // 매칭 id
    private boolean accepted; // 매칭 시 수락 여부
}

