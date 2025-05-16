package com.ssafy.backend.oauth.controller;

import com.ssafy.backend.config.JwtTokenProvider;
import com.ssafy.backend.oauth.dto.KakaoLoginRequest;
import com.ssafy.backend.oauth.dto.KakaoProfileResponse;
import com.ssafy.backend.oauth.service.OAuthService;
import com.ssafy.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;



@RequiredArgsConstructor
@Controller
@RequestMapping("/api/oauth")
public class OAuthController {

    @Qualifier("OAuthKakaoService") // "oauthKakaoService"에서 "OAuthKakaoService"로 수정
    private final OAuthService oauthService;

    private final JwtTokenProvider jwtProvider;

    @Value("${kakao.rest-api-key}")
    private String restKey;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${frontend.url}")
    private String frontendUrl;

    @GetMapping("/kakao")
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

    // 앱에서 직접 액세스 토큰을 전달하는 경우 (기존 코드 유지)
    @PostMapping("/kakao/callback")
    public ResponseEntity<Map<String, String>> handleCallback(
            @RequestBody Map<String, String> body
    ) {
        // 프론트에서 보낸 accessToken 꺼내기
        String accessToken = body.get("accessToken");

        // 1) 카카오 API 로 프로필 조회
        KakaoProfileResponse profile = oauthService.getProfile(accessToken);
        // 2) DB 저장/조회
        User user = oauthService.saveOrUpdateUser(profile);
        // 3) JWT 생성
        String jwt = jwtProvider.generateToken(user.getProvider(), user.getProviderId(), user.getId());

        // 프론트가 기대하는 키 이름(authToken) 으로 반환
        Map<String,String> resp = Map.of("authToken", jwt);
        return ResponseEntity.ok(resp);
    }

    // 웹에서 인가 코드를 전달하는 경우를 위한 새 엔드포인트
    @PostMapping("/kakao/code-callback")
    public ResponseEntity<Map<String, String>> handleCodeCallback(
            @RequestBody Map<String, String> body
    ) {
        // 프론트에서 보낸 인가 코드 꺼내기
        String code = body.get("code");

        if (code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "인가 코드가 필요합니다"));
        }

        try {
            // 1) 인가 코드로 카카오 액세스 토큰 요청 (기존 getToken 메서드 활용)
            KakaoLoginRequest tokenResponse = oauthService.getToken(code);
            String accessToken = tokenResponse.getAccessToken();

            // 2) 카카오 API로 프로필 조회
            KakaoProfileResponse profile = oauthService.getProfile(accessToken);

            // 3) DB 저장/조회
            User user = oauthService.saveOrUpdateUser(profile);

            // 4) JWT 생성
            String jwt = jwtProvider.generateToken(user.getProvider(), user.getProviderId(), user.getId());

            // 프론트가 기대하는 키 이름(authToken)으로 반환
            Map<String, String> resp = Map.of("authToken", jwt);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "인증 처리 중 오류: " + e.getMessage()));
        }
    }
}





//    @GetMapping("/oauth/kakao/callback")
//    public void handleCallback(
//            @RequestParam("code") String code,
//            HttpServletResponse response
//    ) throws IOException {
//        // 1) 인가 코드로 토큰 교환
//        KakaoLoginRequest token   = oauthService.getToken(code);
//        // 2) 토큰으로 프로필 조회
//        KakaoProfileResponse profile = oauthService.getProfile(token.getAccessToken());
//        // 3) DB 저장/조회
//        User user = oauthService.saveOrUpdateUser(profile);
//        // 4) JWT 생성
//        String jwt = jwtProvider.generateToken(user.getProvider(), user.getProviderId());
//
//        // 5) HTTP-Only 쿠키에 JWT 담기 (개발 환경일 땐 secure=false)
//        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", jwt)
//                .httpOnly(true)
//                .secure(false)
//                .sameSite("Lax")
//                .path("/")
//                .maxAge(60 * 60)
//                .build();
//        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
//
//        // 6) 프론트 대시보드로 리다이렉트
//        response.sendRedirect(frontendUrl + "/dashboard");
//    }