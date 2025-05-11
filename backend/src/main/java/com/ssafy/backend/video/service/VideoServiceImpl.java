package com.ssafy.backend.video.service;

import com.ssafy.backend.ai.service.AiService;
import com.ssafy.backend.domain.file.*;
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
    public UploadNotifyResponseDto registerUploadedVideo(UploadNotifyRequestDto dto, Long userId) {

        // 중복 업로드 방지
        videoFileRepository.findByS3Key(dto.getS3Key()).ifPresent(existing -> {
            throw new IllegalStateException("이미 업로드된 파일입니다.");
        });

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 업로드된 파일이 VIDEO인지 PDF인지 자동 분류
        FileType fileType = dto.getContentType().startsWith("video") ? FileType.VIDEO : FileType.PDF;

        // 저장
        VideoFile file = VideoFile.builder()
                .fileName(dto.getFileName())
                .s3Key(dto.getS3Key())
                .contentType(dto.getContentType())
                .size(dto.getSize())
                .uploadType(UploadType.AUTO)
                .fileType(fileType)
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
}