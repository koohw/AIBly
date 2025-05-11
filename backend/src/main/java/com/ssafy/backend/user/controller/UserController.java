package com.ssafy.backend.user.controller;

import com.ssafy.backend.user.dto.response.UserInfoDto;
import com.ssafy.backend.user.entity.User;
import com.ssafy.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

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
}
