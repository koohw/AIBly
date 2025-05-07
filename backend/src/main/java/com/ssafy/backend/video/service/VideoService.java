package com.ssafy.backend.video.service;

import com.ssafy.backend.video.dto.AutoUploadRequestDto;
import com.ssafy.backend.video.dto.AutoUploadResponseDto;

public interface VideoService {
    AutoUploadResponseDto saveAutoUploadedVideo(AutoUploadRequestDto dto, Long userId); // 사용자 정보 연동 가능
}
