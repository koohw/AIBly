package com.ssafy.backend.fcm.service;

import com.ssafy.backend.fcm.entity.FcmToken;
import com.ssafy.backend.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FcmTokenServiceImpl implements FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public void registerOrUpdateFcmToken(Long userId, String fcmToken) {
        fcmTokenRepository.findByUserId(userId).ifPresentOrElse(
                token -> {
                    token.updateToken(fcmToken);
                    fcmTokenRepository.save(token);
                },
                () -> {
                    FcmToken newToken = FcmToken.builder()
                            .userId(userId)
                            .token(fcmToken)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    fcmTokenRepository.save(newToken);
                }
        );
    }
}
