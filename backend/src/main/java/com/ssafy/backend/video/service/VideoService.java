package com.ssafy.backend.video.service;

import com.ssafy.backend.domain.file.UploadType;
import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;
import com.ssafy.backend.video.dto.response.MyVideoResponseDto;
import com.ssafy.backend.video.dto.response.UploadNotifyResponseDto;

import java.util.List;


public interface VideoService {
    UploadNotifyResponseDto registerUploadedVideo(UploadNotifyRequestDto dto, Long userId, UploadType uploadType); // 업로드가 완료된 영상을 등록하고 분석해라

    UploadType getUploadType(Long videoId); //파일 업로드에 따라 fcm,polling 사용 여부 다름

    List<MyVideoResponseDto> getMyVideos(Long userId); //내 영상 목록

    MyVideoResponseDto getMyVideoDetail(Long userId, Long videoId); //내 영상 상세조회

    void deleteVideo(Long videoId, boolean alsoDeleteS3);  // 영상 삭제 시 참조 확인 후 S3 삭제

    // 내 영상함에서 영상 삭제 (보고서 + PDF 포함 전부 삭제)
    void deleteVideoAndReport(Long userId, Long videoId);

}
