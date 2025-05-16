package com.ssafy.backend.report.service;

import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.report.Report;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.report.dto.response.ReportDetailResponseDto;
import com.ssafy.backend.report.dto.response.ReportListResponseDto;
import com.ssafy.backend.s3.service.S3UploadService;
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


    //보고서 삭제
    @Transactional
    @Override
    public void deleteMyReport(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndVideoFileUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        reportRepository.findVideoS3KeyByReportId(reportId)
                .ifPresent(s3Key -> s3UploadService.deleteS3File(userId, s3Key));

        reportRepository.findPdfKeyByReportId(reportId)
                .filter(pdfKey -> pdfKey != null && !pdfKey.isEmpty())
                .ifPresent(pdfKey -> s3UploadService.deleteS3File(userId, pdfKey));

        reportRepository.delete(report);
    }
}
