package com.ssafy.backend.common.controller;

import com.ssafy.backend.config.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseController {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    protected Long getCurrentUserId(HttpServletRequest request) {
        String token = extractTokenFromCookie(request);
        return jwtTokenProvider.getUserIdFromToken(token);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new IllegalStateException("쿠키가 없습니다.");
        }
        for (Cookie cookie : cookies) {
            if ("AUTH_TOKEN".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        throw new IllegalStateException("AUTH_TOKEN 쿠키가 없습니다.");
    }
}