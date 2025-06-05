package com.ssafy.backend.healthcheck.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
public class HealthCheckController {

    @GetMapping("")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok("OK");
    }
}
