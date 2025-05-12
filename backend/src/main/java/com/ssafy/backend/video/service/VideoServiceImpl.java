package com.ssafy.backend.video.service;

import com.ssafy.backend.ai.service.AiService;
import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.file.*;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import com.ssafy.backend.user.entity.User;
import com.ssafy.backend.user.repository.UserRepository;


import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;
import com.ssafy.backend.video.dto.response.UploadNotifyResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoFileRepository videoFileRepository;
    private final UserRepository userRepository;
    private final AiService aiService;

    @Override
    @Transactional
    public UploadNotifyResponseDto registerUploadedVideo(UploadNotifyRequestDto dto, Long userId, UploadType uploadType) {

        // 중복 업로드 방지
        videoFileRepository.findByUserIdAndS3Key(userId, dto.getS3Key()).ifPresent(existing -> {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        });


        // 실제 User 엔티티 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


        // 업로드된 파일이 VIDEO인지 PDF인지 자동 분류
        FileType fileType = determineFileType(dto.getContentType());

        // 저장
        VideoFile file = VideoFile.builder()
                .fileName(dto.getFileName())
                .s3Key(dto.getS3Key())
                .contentType(dto.getContentType())
                .size(dto.getSize())
                .uploadType(uploadType)         // AUTO / MANUAL
                .fileType(fileType)             // VIDEO / PDF
                .analysisStatus(AnalysisStatus.ANALYZING)
                .user(user)
                .build();
        videoFileRepository.save(file);

        //AI 분석 요청
        if (fileType == FileType.VIDEO) {
            aiService.requestAnalysis(file);
        }

        // 5. 저장된 정보를 응답 DTO로 반환
        return new UploadNotifyResponseDto(file.getId(), file.getFileType(), file.getAnalysisStatus());
    }

    //FileType 분류
    private FileType determineFileType(String contentType) {
        if (contentType != null && contentType.startsWith("video")) {
            return FileType.VIDEO;
        }
        return FileType.PDF;
    }
}