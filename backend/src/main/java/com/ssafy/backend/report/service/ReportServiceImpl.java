package com.ssafy.backend.report.service;

import com.ssafy.backend.common.exception.CustomException;
import com.ssafy.backend.common.exception.ErrorCode;
import com.ssafy.backend.domain.report.ReportRepository;
import com.ssafy.backend.domain.report.projection.ReportDetailProjection;
import com.ssafy.backend.report.dto.response.ReportDetailResponseDto;
import com.ssafy.backend.report.dto.response.ReportListResponseDto;
import com.ssafy.backend.s3.service.S3UploadService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    /**
     * 보고서 목록 조회 (Projection → DTO)
     */
    @Transactional(readOnly = true)
    @Override
    public List<ReportListResponseDto> getMyReports(Long userId) {
        return reportRepository.findAllByUserId(userId)
                .stream()
                .map(p -> new ReportListResponseDto(
                        p.getId(),
                        p.getTitle(),
                        p.getAccidentCode(),
                        p.getCreatedAt().toString()
                ))
                .toList();
    }

    /**
     * 보고서 상세 조회 (Projection → DTO)
     */
    @Transactional(readOnly = true)
    @Override
    public ReportDetailResponseDto getMyReportDetail(Long userId, Long reportId) {
        ReportDetailProjection projection = reportRepository.findDetailByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 보고서를 찾을 수 없습니다."));

        return new ReportDetailResponseDto(
                projection.getId(),
                projection.getTitle(),
                projection.getAccidentType(),
                projection.getCarA(),
                projection.getCarB(),
                projection.getMainEvidence(),
                projection.getLaws(),
                projection.getDecisions(),
                projection.getCreatedAt().toString(),
                projection.getPdfKey(),
                projection.getFileId()
        );
    }

    /**
     * 보고서 삭제 (Projection으로 S3 키 조회 → S3 삭제 → DB 삭제)
     */
    @Transactional
    @Override
    public void deleteMyReport(Long userId, Long reportId) {
        // Projection으로 존재 확인
        reportRepository.findIdByReportIdAndUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        // S3 Video 삭제
        reportRepository.findVideoS3KeyByReportId(reportId)
                .ifPresent(s3Key -> s3UploadService.deleteS3File(userId, s3Key));

        // PDF 삭제
        reportRepository.findPdfKeyByReportId(reportId)
                .filter(pdfKey -> pdfKey != null && !pdfKey.isEmpty())
                .ifPresent(pdfKey -> s3UploadService.deleteS3File(userId, pdfKey));

        // Hibernate Dirty Checking 없는 안전한 삭제
        reportRepository.deleteById(reportId);
    }
}

