package com.example.match.controller;



import com.example.match.feign.ServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/match")
public class TestController {

    private final ServiceClient serviceClient;

    public TestController(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @GetMapping("test")
    public ResponseEntity<?> test(@RequestHeader("X-User-ID") String serialNumber) {

        System.out.println("실행도ㅣ나...");
        return ResponseEntity.ok(serviceClient.getUser(serialNumber));
    }

    @GetMapping({"", "/"})
    public ResponseEntity<?> test2() {

        serviceClient.test();

        return ResponseEntity.ok("굿");
    }

}
