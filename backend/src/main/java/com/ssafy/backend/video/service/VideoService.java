package com.ssafy.backend.video.service;

import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;
import com.ssafy.backend.video.dto.response.UploadNotifyResponseDto;


public interface VideoService {
    UploadNotifyResponseDto registerUploadedVideo(UploadNotifyRequestDto dto, Long userId, UploadType uploadType); // 업로드가 완료된 영상을 등록하고 분석해라

    UploadType getUploadType(Long videoId); //파일 업로드에 따라 fcm,polling 사용 여부 다름

}
