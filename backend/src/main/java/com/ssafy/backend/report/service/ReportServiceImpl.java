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
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final S3UploadService s3UploadService;

    //보고서 목록 조회
    @Override
    public List<ReportListResponseDto> getMyReports(Long userId) {
        return reportRepository.findByVideoFileUserId(userId)
                .stream()
                .map(ReportListResponseDto::from)
                .toList();
    }

    //보고서 상세 조회
    @Override
    public ReportDetailResponseDto getMyReportDetail(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndVideoFileUserId(reportId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 보고서를 찾을 수 없습니다."));
        return ReportDetailResponseDto.from(report);
    }

    //사용자가 보고서 삭제하면 s3에 해당되는 파일도 같이 연쇄 삭제
    @Transactional
    @Override
    public void deleteMyReport(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndVideoFileUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        // PDF S3 삭제 (있으면)
        if (report.getPdfKey() != null) {
            try {
                s3UploadService.deleteS3File(userId, report.getPdfKey());
            } catch (Exception e) {
                throw new CustomException(ErrorCode.S3_DELETE_FAIL);
            }
        }

        // 영상 S3 삭제 (있으면)
        if (report.getVideoFile() != null && report.getVideoFile().getS3Key() != null) {
            try {
                s3UploadService.deleteS3File(userId, report.getVideoFile().getS3Key());
            } catch (Exception e) {
                throw new CustomException(ErrorCode.S3_DELETE_FAIL);
            }
        }

        // Report 삭제
        reportRepository.delete(report);
    }

}
