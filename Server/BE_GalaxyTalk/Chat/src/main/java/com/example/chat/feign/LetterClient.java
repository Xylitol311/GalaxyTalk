package com.example.chat.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "support-service")
public interface LetterClient {
    @GetMapping("/api/letter/chat/{chatRoomId}")
    ResponseEntity<?> getLetter(@RequestHeader("X-User-Id") String serialNumber,
                                 @PathVariable("chatRoomId") String chatRoomId);
}
