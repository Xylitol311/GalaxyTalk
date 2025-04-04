package com.example.chat.feign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="auth-service")
// 유레카에 등록된 서비스 이름
public interface AuthClient {

    //api 경로 & controller에 등록된 정보 가져오기
    @GetMapping("/api/oauth")
    ResponseEntity<?> getUser(@RequestHeader("X-User-ID") String serialNumber);

    @PostMapping("/api/oauth/status")
    ResponseEntity<?> changeUserStatus(@RequestHeader("X-User-ID") String serialNumber, @RequestBody String userInteractionState);
}
