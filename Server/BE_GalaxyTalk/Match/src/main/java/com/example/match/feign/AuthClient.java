package com.example.match.feign;

import com.example.match.dto.UserStatusRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="auth-service")
public interface AuthClient {

    @GetMapping("/api/oauth")
    ResponseEntity<?> getUser(@RequestHeader("X-User-ID") String serialNumber);

    @PostMapping(value = "/api/oauth/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> changeUserStatus(@RequestHeader("X-User-ID") String serialNumber,
                                       @RequestBody UserStatusRequestDto userStatusRequest);
}
