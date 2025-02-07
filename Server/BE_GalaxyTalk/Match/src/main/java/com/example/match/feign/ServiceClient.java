package com.example.match.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name="auth-service")
public interface ServiceClient {

    @GetMapping("/api/oauth")
    String getUser(@RequestHeader("X-User-ID") String serialNumber);

    @GetMapping("/api/oauth/test")
    String test();
}

