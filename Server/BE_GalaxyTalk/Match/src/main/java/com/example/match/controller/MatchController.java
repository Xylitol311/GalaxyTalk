package com.example.match.controller;

import com.example.match.dto.MatchRequestDto;
import com.example.match.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;

    @PostMapping("/api/match/start")
    public ResponseEntity<?> startMatching(@RequestBody MatchRequestDto request) {
//        UserMatchStatus status = convertToStatus(request);
//        matchService.startMatching(status);
        return ResponseEntity.ok().build();
    }

}
