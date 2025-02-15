package com.galaxytalk.letter.feign;

import com.galaxytalk.letter.dto.EnergyRequest;
import feign.Feign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name="auth-service")
// 유레카에 등록된 서비스 이름
public interface AuthClient {

    @PostMapping("/api/oauth/energy")
    public ResponseEntity<?> increaseEnergy(@RequestBody EnergyRequest energyRequest);

    @GetMapping("/api/oauth")
    public ResponseEntity<?> getUserInfo(@RequestHeader("X-User-ID") String serialNumber);

}
