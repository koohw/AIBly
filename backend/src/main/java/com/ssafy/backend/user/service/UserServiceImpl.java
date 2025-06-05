package com.ssafy.backend.user.service;

import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import com.ssafy.backend.fcm.entity.FcmToken;
import com.ssafy.backend.fcm.repository.FcmTokenRepository;
import com.ssafy.backend.user.entity.User;
import com.ssafy.backend.user.repository.UserRepository;
import com.ssafy.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final VideoFileRepository videoFileRepository;
    private final FcmTokenRepository fcmTokenRepository;


    //FE가 호출하는 게 아니라 BE 내부 서비스에서만 사용
    //FE(앱) → BE에게 내 FCM 토큰을 등록하기 위한 API
    @Override
    public void updateUserFcmToken(Long userId, String fcmToken) {
        fcmTokenRepository.findByUserId(userId).ifPresentOrElse(
                token -> {
                    token.updateToken(fcmToken); // 기존 토큰 있으면 새로 받은 토큰으로 업데이트
                    fcmTokenRepository.save(token);
                },
                () -> {
                    FcmToken newToken = FcmToken.builder()
                            .userId(userId)
                            .token(fcmToken)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    fcmTokenRepository.save(newToken);// 없으면 새로 저장 (신규 등록)
                }
        );
    }

    //FCM Token 조회
    //FCM Token은 "디바이스 식별" 용도 → 항상 FcmTokenRepository로 관리
    @Override
    public String getUserFcmTokenByVideoId(Long videoId) {
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));

        Long userId = videoFile.getUser().getId();

        return fcmTokenRepository.findByUserId(userId)
                .map(FcmToken::getToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT_VALUE));
    }

    @Override
    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
        // FCM 토큰도 함께 삭제 (권장)
        fcmTokenRepository.findByUserId(userId)
                .ifPresent(fcmTokenRepository::delete);
    }



}
