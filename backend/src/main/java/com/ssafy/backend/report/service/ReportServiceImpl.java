package com.ssafy.backend.report.service;

import com.ssafy.backend.ai.service.AiService;
import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.domain.video.VideoFile;
import com.ssafy.backend.domain.video.VideoFileRepository;
import com.ssafy.backend.report.dto.response.ReportDetailResponseDto;
import com.ssafy.backend.report.dto.response.ReportListResponseDto;
import com.ssafy.backend.s3.service.S3UploadService;
import com.ssafy.backend.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final S3UploadService s3UploadService;
    private final VideoService videoService;
    private final AiService aiService;
    private final VideoFileRepository videoFileRepository;

    //내 영상에서 보고서 재생성
    @Transactional
    public void recreateReport(Long videoId, Long userId) {
        VideoFile videoFile = videoFileRepository.findByIdAndUserId(videoId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));

        boolean reportExists = reportRepository.existsByVideoFileId(videoFile.getId());
        if (reportExists) {
            throw new CustomException(ErrorCode.REPORT_ALREADY_EXISTS);
        }

        aiService.requestAndHandleAnalysis(videoFile);
    }

    //보고서 목록 조회
    @Transactional(readOnly = true)
    @Override
    public List<ReportListResponseDto> getMyReports(Long userId) {
        return reportRepository.findAllByUserId(userId)
                .stream()
                .map(report -> ReportListResponseDto.from(report))
                .toList();
    }


    //보고서 상세 조회 (Projection → DTO)
    @Override
    public ReportDetailResponseDto getMyReportDetail(Long userId, Long reportId) {
        Report report = reportRepository.findDetailByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 보고서를 찾을 수 없습니다."));

        //S3 Presigned URL 생성 추가
        String videoUrl = s3UploadService.generateDownloadPresignedUrl(report.getVideoFile().getS3Key());

        return ReportDetailResponseDto.from(report, videoUrl);
    }

    //보고서 삭제(pdf만 삭제, 영상도 삭제 등 -> 조건 있음)
    @Transactional
    @Override
    public void deleteMyReport(Long userId, Long reportId, boolean alsoDeleteVideo) {
        Report report = reportRepository.findByIdAndVideoFileUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        // PDF 존재 시 삭제
        reportRepository.findPdfKeyByReportId(reportId)
                .filter(pdfKey -> pdfKey != null && !pdfKey.isEmpty())
                .ifPresent(pdfKey -> {
                    s3UploadService.deleteFromS3(pdfKey);
                    s3UploadService.deleteS3FileEntity(pdfKey);
                });
        // Report 먼저 삭제, 보고서 다음 영상 삭제 순으로 해야 제약조건에 위배되지 않음
        Long videoId = report.getVideoFile().getId();  // 먼저 id 확보
        reportRepository.delete(report);

        // 체크된 경우 영상도 같이 삭제
        if (alsoDeleteVideo && report.getVideoFile() != null) {
            videoService.deleteVideo(videoId, true); // Report 삭제 이후 안전하게 video 삭제
        }
    }

    // pdf 한번 다운 받은 후 두번째부터
    @Override
    @Transactional(readOnly = true)
    public String getPdfPresignedUrl(Long userId, Long reportId) {
        Report report = reportRepository.findDetailByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        String pdfKey = report.getPdfKey();
        if (pdfKey == null || pdfKey.isEmpty()) {
            throw new CustomException(ErrorCode.PDF_NOT_FOUND); // 필요시 추가
        }

        return s3UploadService.generateDownloadPresignedUrl(pdfKey);
    }

}
