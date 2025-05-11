package com.ssafy.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        // 0) 필터 진입 로그
        System.out.println("[JwtFilter] ▶  URI = " + request.getRequestURI());

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            System.out.println("[JwtFilter] ▶ no cookie");
        } else {
            boolean found = false;
            for (Cookie cookie : cookies) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    found = true;
                    String token = cookie.getValue();
                    System.out.println("[JwtFilter] ▶ AUTH_TOKEN : " + token);

                    // 2) 토큰 유효성 검사
                    if (jwtProvider.validateToken(token)) {
                        System.out.println("[JwtFilter] ▶ token yes");
                        // 3) 토큰으로부터 Authentication 생성
                        Authentication auth = jwtProvider.getAuthentication(token);
                        System.out.println("[JwtFilter] ▶ Authentication principal = " +
                                auth.getPrincipal());
                        // 4) SecurityContext 에 등록
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        System.out.println("[JwtFilter] ▶ token no");
                    }
                    break;
                }
            }
            if (!found) {
                System.out.println("[JwtFilter] ▶ AUTH_TOKEN 쿠키가 존재하지 않습니다");
            }
        }

        // 다음 필터/컨트롤러로 진행
        chain.doFilter(request, response);
    }
}
