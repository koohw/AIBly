package com.ssafy.backend.oauth.controller;

import com.ssafy.backend.config.JwtTokenProvider;
import com.ssafy.backend.oauth.dto.KakaoLoginRequest;
import com.ssafy.backend.oauth.dto.KakaoProfileResponse;
import com.ssafy.backend.oauth.dto.JwtResponse;
import com.ssafy.backend.oauth.service.OAuthService;
import com.ssafy.backend.user.dto.response.UserInfoDto;
import com.ssafy.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


@RequiredArgsConstructor
@Controller
public class OAuthController {

    @Qualifier("oauthKakaoService")
    private final OAuthService oauthService;

    private final JwtTokenProvider jwtProvider;

    @Value("${kakao.rest-api-key}")
    private String restKey;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${frontend.url}")
    private String frontendUrl;

    @GetMapping("/api/auth/kakao")
    public void redirectToKakao(HttpServletResponse response) throws IOException {
        String uri = UriComponentsBuilder
                .fromHttpUrl("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", restKey)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .build()
                .toUriString();
        response.sendRedirect(uri);
    }

    @GetMapping("/oauth/kakao/callback")
    public void handleCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {
        // 1) 인가 코드로 토큰 교환
        KakaoLoginRequest token   = oauthService.getToken(code);
        // 2) 토큰으로 프로필 조회
        KakaoProfileResponse profile = oauthService.getProfile(token.getAccessToken());
        // 3) DB 저장/조회
        User user = oauthService.saveOrUpdateUser(profile);
        // 4) JWT 생성
        String jwt = jwtProvider.generateToken(user.getProvider(), user.getProviderId());

        // 5) HTTP-Only 쿠키에 JWT 담기 (개발 환경일 땐 secure=false)
        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", jwt)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 6) 프론트 대시보드로 리다이렉트
        response.sendRedirect(frontendUrl + "/dashboard");
    }
}