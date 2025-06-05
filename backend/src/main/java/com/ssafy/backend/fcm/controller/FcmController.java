package com.ssafy.backend.fcm.controller;

import com.ssafy.backend.config.CustomUserDetails;
import com.ssafy.backend.fcm.service.FcmService;
import com.ssafy.backend.fcm.service.FcmTokenService;
import com.ssafy.backend.user.dto.response.request.FcmTokenRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    public ResponseEntity<Void> registerFcmToken(@RequestBody FcmTokenRequestDto requestDto,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        fcmTokenService.registerOrUpdateFcmToken(user.getUserId(), requestDto.getFcmToken());
        return ResponseEntity.ok().build();
    }

}
//사용자가 로그인 직후 앱에서 FCM Token 받음
//FCM Token은 디바이스/앱 기준으로 발급
//사용자가 앱 재설치, 로그아웃 후 재로그인시 토큰 재발급
