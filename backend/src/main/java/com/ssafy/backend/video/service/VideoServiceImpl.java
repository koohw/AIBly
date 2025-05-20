package com.ssafy.backend.video.service;

import com.ssafy.backend.ai.service.AiService;
import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.file.*;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import com.ssafy.backend.s3.service.S3UploadService;
import com.ssafy.backend.user.entity.User;
import com.ssafy.backend.user.repository.UserRepository;


import com.ssafy.backend.video.dto.request.UploadNotifyRequestDto;
import com.ssafy.backend.video.dto.response.MyVideoResponseDto;
import com.ssafy.backend.video.dto.response.UploadNotifyResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl implements VideoService {

    private final VideoFileRepository videoFileRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final S3UploadService s3UploadService;
    private final ReportRepository reportRepository;


    @Override
    @Transactional
    public UploadNotifyResponseDto registerUploadedVideo(UploadNotifyRequestDto dto, Long userId, UploadType uploadType) {

        // 1. 중복 업로드 방지
        videoFileRepository.findByUserIdAndS3KeyAndFileName(userId, dto.getS3Key(),dto.getFileName()).ifPresent(existing -> {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE); // 이미 등록된 파일
        });

        // 2. 실제 User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 3. 사전에 저장된 S3File 확인 (이게 없으면 등록 불가)
        S3File s3File = s3UploadService.getS3FileByS3KeyAndUserId(dto.getS3Key(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        // 4. 파일 타입 자동 분류
        FileType fileType = determineFileType(s3File.getContentType());

        // 5. VideoFile 생성
        VideoFile file = VideoFile.builder()
                .fileName(dto.getFileName())
                .s3Key(s3File.getS3Key())
                .contentType(s3File.getContentType())
                .size(s3File.getSize())
                .uploadType(uploadType)
                .fileType(fileType)
                .analysisStatus(AnalysisStatus.ANALYZING)
                .user(user)
                .build();

        videoFileRepository.save(file);

        // 6. AI 분석 요청 (영상인 경우에만) - 비동기로 처리
        if (fileType == FileType.VIDEO) {
            log.info("비디오 ID: {}에 대한 AI 분석 요청을 비동기로 처리합니다.", file.getId());
            requestAnalysisAsync(file.getId());
        }

        // 7. 결과 즉시 반환
        return new UploadNotifyResponseDto(file.getId(), file.getFileType(), file.getAnalysisStatus());
    }

    // 비동기 분석 요청 메서드 추가
    @Async
    public void requestAnalysisAsync(Long videoId) {
        try {
            log.info("비동기 AI 분석 시작: 비디오 ID {}", videoId);
            VideoFile file = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));
            
            aiService.requestAndHandleAnalysis(file);
            log.info("비동기 AI 분석 요청 완료: 비디오 ID {}", videoId);
        } catch (Exception e) {
            // 비동기 처리 중 발생한 예외 로깅
            log.error("AI 분석 요청 중 오류 발생 (비디오 ID: {}): {}", videoId, e.getMessage(), e);
            
            // 오류 상태로 업데이트
            try {
                VideoFile file = videoFileRepository.findById(videoId).orElse(null);
                if (file != null) {
                    file.setAnalysisStatus(AnalysisStatus.FAILED);
                    videoFileRepository.save(file);
                    log.info("비디오 ID: {}의 상태를 FAILED로 업데이트했습니다.", videoId);
                }
            } catch (Exception updateEx) {
                log.error("상태 업데이트 중 오류 발생: {}", updateEx.getMessage(), updateEx);
            }
        }
    }

    //FileType 분류
    private FileType determineFileType(String contentType) {
        if (contentType != null && contentType.startsWith("video")) {
            return FileType.VIDEO;
        }
        return FileType.PDF;
    }

    //파일 업로드에 따른 fcm, polling 처리 다름
    @Override
    public UploadType getUploadType(Long videoId) {
        VideoFile videoFile = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));
        return videoFile.getUploadType();
    }

    @Override
    public List<MyVideoResponseDto> getMyVideos(Long userId) {
        List<VideoFile> videos = videoFileRepository.findByUserId(userId);

        return videos.stream()
                .map(video -> MyVideoResponseDto.builder()
                        .videoId(video.getId())
                        .fileName(video.getFileName())
                        .videoUrl(s3UploadService.generateDownloadPresignedUrl(video.getS3Key()))
                        .uploadedAt(video.getUploadedAt())
                        .size(video.getSize())
                        .build())
                .toList();
    }

    @Override
    public MyVideoResponseDto getMyVideoDetail(Long userId, Long videoId) {
        VideoFile video = videoFileRepository.findByIdAndUserId(videoId, userId)
                .orElseThrow(() -> new IllegalArgumentException("영상이 존재하지 않습니다."));

        return MyVideoResponseDto.builder()
                .videoId(video.getId())
                .fileName(video.getFileName())
                .videoUrl(s3UploadService.generateDownloadPresignedUrl(video.getS3Key()))
                .uploadedAt(video.getUploadedAt())
                .size(video.getSize())
                .build();
    }

    //영상 삭제 시 참조 확인 후 S3 삭제
    //다른 파일명의 같은 영상이 존재하면 삭제 안함
    @Transactional
    @Override
    public void deleteVideo(Long videoId, boolean alsoDeleteS3) {
        VideoFile video = videoFileRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));

        String s3Key = video.getS3Key();
        videoFileRepository.delete(video);

        if (alsoDeleteS3) {
            long remaining = videoFileRepository.countByS3Key(s3Key);
            if (remaining == 0) {
                s3UploadService.deleteFromS3(s3Key);              // S3 객체 삭제
                s3UploadService.deleteS3FileEntity(s3Key);        // s3_files 레코드 삭제
            }
        }
    }

    @Override
    @Transactional
    public void deleteVideoAndReport(Long userId, Long videoId) {
        VideoFile video = videoFileRepository.findByIdAndUserId(videoId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));

        // 연결된 보고서 삭제
        reportRepository.findByVideoFileId(videoId).ifPresent(report -> {
            // PDF도 삭제
            String pdfKey = report.getPdfKey();
            if (pdfKey != null && !pdfKey.isEmpty()) {
                s3UploadService.deleteFromS3(pdfKey);
                s3UploadService.deleteS3FileEntity(pdfKey);
            }

            reportRepository.delete(report);
        });

        // videoFile 삭제
        videoFileRepository.delete(video);

        // S3Key 참조 수 확인
        String s3Key = video.getS3Key();
        long remaining = videoFileRepository.countByS3Key(s3Key);
        if (remaining == 0) {
            s3UploadService.deleteFromS3(s3Key);
            s3UploadService.deleteS3FileEntity(s3Key);
        }
    }

}
