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

    //fcm
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


}
