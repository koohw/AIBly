package com.ssafy.backend.oauth.controller;

import com.ssafy.backend.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TestAuthController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 테스트용 토큰 발급
     */
    @PostMapping("/login/test")
    public ResponseEntity<String> testLogin() {
        // 💡 여기서 userId는 테스트용으로 DB에 넣어둔 사용자 ID (예: 1L)
        String token = jwtTokenProvider.generateToken("kakao", "테스트_임시_ID", 1L);
        return ResponseEntity.ok(token);
    }
}
