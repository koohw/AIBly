package com.ssafy.backend.user.controller;

import com.ssafy.backend.common.controller.BaseController;
import com.ssafy.backend.config.CustomUserDetails;
import com.ssafy.backend.user.dto.response.UserInfoDto;
import com.ssafy.backend.config.JwtTokenProvider;
import com.ssafy.backend.user.dto.response.request.FcmTokenRequestDto;
import com.ssafy.backend.user.service.UserService;
import com.ssafy.backend.user.entity.User;
import com.ssafy.backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserRepository userRepository;

    private final JwtTokenProvider jwtProvider;

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserInfoDto> getCurrentUser(Principal principal) {
        // 1) 인증된 principal.getName()은 "provider:providerId" 형태
        String[] parts = principal.getName().split(":");
        String provider   = parts[0];
        String providerId = parts[1];

        // 2) DB에서 실제 User 엔티티 조회
        User user = userRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3) DTO로 변환해 반환
        UserInfoDto dto = new UserInfoDto(user.getId(), user.getName(), user.getEmail());
        return ResponseEntity.ok(dto);
    }

    // DELETE /api/user/delete
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUser(@RequestHeader("Authorization") String authorizationHeader) {
        // "Bearer {token}" 형태일 때 토큰만 분리
        String token = authorizationHeader.replaceFirst("^Bearer\\s+", "");
        Long userId = jwtProvider.getUserIdFromToken(token);

        userService.deleteUserById(userId);
        return ResponseEntity.noContent().build();
    }


    //FE가 로그인 후 → BE에 FCM 토큰을 등록할 수 있는 API
    @PostMapping("/fcm-token")
    public ResponseEntity<Void> registerFcmToken(@RequestBody FcmTokenRequestDto requestDto,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        userService.updateUserFcmToken(user.getUserId(), requestDto.getFcmToken());
        return ResponseEntity.ok().build();
    }


}
