package com.ssafy.backend.video.service;

import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;
<<<<<<< HEAD
=======
import com.ssafy.backend.video.dto.UploadNotifyResponseDto;
>>>>>>> origin/develop

public interface VideoService {
    UploadNotifyResponseDto registerUploadedVideo(UploadNotifyRequestDto dto, Long userId); // 업로드가 완료된 영상을 등록하고 분석
}
