package com.example.match.controller;

import com.example.match.domain.MatchResponse;
import com.example.match.domain.MatchStatus;
import com.example.match.domain.UserMatchStatus;
import com.example.match.dto.MatchRequestDto;
import com.example.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    // 매칭 시작 요청
    @PostMapping("/api/match/start")
    public ResponseEntity<?> startMatching(@RequestBody MatchRequestDto request) {
        request.setUserId("헤더에서 유저 정보 추출하여 입력");
        UserMatchStatus status = convertToStatus(request); //redis에 저장할 객체로 변환
        matchService.startMatching(status);
        return ResponseEntity.ok().build();
    }

    // 매칭 수락 여부 응답
    @MessageMapping("/match/response")
    public void handleMatchResponse(MatchResponse response) {
        matchService.processMatchResponse(response);
    }

    // 매칭 요청 정보를 redis에 저장할 객체 형태로 변환
    private UserMatchStatus convertToStatus(MatchRequestDto dto) {
        UserMatchStatus status = new UserMatchStatus();
        status.setUserId(dto.getUserId());
        status.setConcern(dto.getConcern());
        status.setPreferredMbti(dto.getPreferredMbti());
        status.setAge(dto.getAge());
        status.setStatus(MatchStatus.WAITING);
        status.setAccepted(false);
        return status;
    }
}
