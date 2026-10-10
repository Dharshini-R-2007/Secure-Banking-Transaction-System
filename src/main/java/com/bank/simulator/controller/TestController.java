package com.bank.simulator.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/admin/test")
    public ResponseEntity<Map<String, String>> adminTest(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "message", "BANK_ADMIN endpoint accessed successfully!",
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }

    @GetMapping("/client/test")
    public ResponseEntity<Map<String, String>> clientTest(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "message", "CLIENT endpoint accessed successfully!",
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }

    @GetMapping("/user/test")
    public ResponseEntity<Map<String, String>> userTest(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "message", "Authenticated user endpoint accessed successfully!",
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }
}
