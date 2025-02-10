package com.example.match.domain;

import lombok.Data;

import java.util.List;

@Data
// 매칭 정보(유저, 유사도 점수)를 저장하는 객체
public class MatchResultStatus {
    List<String> userIds;
    double similarity;
}
