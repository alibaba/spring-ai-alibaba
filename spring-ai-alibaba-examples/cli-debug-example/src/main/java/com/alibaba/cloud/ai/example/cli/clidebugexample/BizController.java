package com.alibaba.cloud.ai.example.cli.clidebugexample;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class BizController {
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("ok");
    }
}
