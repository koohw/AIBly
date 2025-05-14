package com.ssafy.backend.user.service;

import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import com.ssafy.backend.user.entity.User;
import com.ssafy.backend.user.repository.UserRepository;
import com.ssafy.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final VideoFileRepository videoFileRepository;

    @Override
    public void deleteUserById(Long userId) {
        userRepository.deleteById(userId);
    }

    //FE가 호출하는 게 아니라 BE 내부 서비스에서만 사용
    //AI 콜백 처리 중에서 videoId 기준으로 user → FCM Token 조회
    @Override
    public String getUserFcmTokenByVideoId(Long videoId) {
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));

        User user = videoFile.getUser();
        if (user.getFcmToken() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return user.getFcmToken();
    }

    //FE(앱) → BE에게 내 FCM 토큰을 등록하기 위한 API
    @Override
    public void updateUserFcmToken(Long userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }



}
