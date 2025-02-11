package com.example.match.feign;

import com.example.match.dto.MatchResultRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name="chat-service")
public interface ChatClient {

    @PostMapping("/api/chat/match")
    ResponseEntity<?> createSessionWithTwoTokens(@RequestBody MatchResultRequest matchResultRequest);
}
