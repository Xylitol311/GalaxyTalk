package com.example.support.test;


import com.example.support.feign.ServiceClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
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
    public ResponseEntity<String> test2() {
        System.out.println("실행됨?");
        return serviceClient.test();
    }

}
