package com.ssafy.backend.common.controller;

import com.ssafy.backend.config.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * 요청에서 JWT 추출 → 사용자 ID 반환
     * - Authorization 헤더 > Cookie 순으로 검사
     */
    protected Long getCurrentUserId(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            throw new IllegalStateException("인증 토큰이 없습니다.");
        }
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더 확인
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        // 2. Cookie 확인 (웹 대응)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 3. 둘 다 없으면 null
        return null;
    }
}